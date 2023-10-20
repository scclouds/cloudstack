// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.gui.theme;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.user.gui.themes.CreateGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.themes.ListGuiThemesCmd;
import org.apache.cloudstack.api.command.user.gui.themes.RemoveGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.themes.UpdateGuiThemeCmd;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.dao.GuiThemeDao;
import org.apache.cloudstack.gui.themes.GuiThemeVO;
import org.apache.cloudstack.gui.themes.GuiThemeService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuiThemeServiceImpl implements GuiThemeService {

    protected Logger logger = Logger.getLogger(GuiThemeServiceImpl.class);

    private static final List<String> ALLOWED_PRIMITIVE_PROPERTIES = List.of("appTitle", "footer", "loginFooter", "logo", "minilogo", "banner");

    private static final List<String> ALLOWED_ERROR_PROPERTIES = List.of("403", "404", "500");

    private static final List<String> ALLOWED_PLUGIN_PROPERTIES = List.of("name", "path", "icon", "isExternalLink");

    private static final String ERROR = "error";

    private static final String PLUGINS = "plugins";

    @Inject
    GuiThemeDao guiThemeDao;

    @Inject
    ResponseGenerator responseGenerator;

    @Inject
    EntityManager entityManager;

    @Inject
    AccountDao accountDao;

    @Inject
    DomainDao domainDao;

    @Override
    public ListResponse<GuiThemeResponse> listGuiThemes(ListGuiThemesCmd cmd) {
        ListResponse<GuiThemeResponse> response = new ListResponse<>();
        Pair<List<GuiThemeVO>, Integer> result;
        boolean listOnlyDefaultTheme = cmd.getListOnlyDefaultTheme();

        if (listOnlyDefaultTheme) {
            result = retrieveDefaultTheme();
        } else if (CallContext.current().getCallingAccountId() == Account.ACCOUNT_ID_SYSTEM) {
            logger.info("Unauthenticated call to `listGuiThemes` API, ignoring all parameters, except `commonName`.");
            result = listGuiThemesWithNoAuthentication(cmd);
        } else {
            result = listGuiThemesInternal(cmd);
        }
        List<GuiThemeResponse> guiThemeResponses = new ArrayList<>();

        for (GuiThemeVO guiThemeVO : result.first()) {
            GuiThemeResponse guiThemeResponse = responseGenerator.createGuiThemeResponse(guiThemeVO);
            guiThemeResponses.add(guiThemeResponse);
        }

        response.setResponses(guiThemeResponses);
        return response;
    }

    private Pair<List<GuiThemeVO>, Integer> retrieveDefaultTheme() {
        GuiThemeVO defaultTheme = guiThemeDao.findDefaultTheme();
        List<GuiThemeVO> list = new ArrayList<>();

        if (defaultTheme != null) {
            list.add(defaultTheme);
        }

        return new Pair<>(list, list.size());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_CREATE, eventDescription = "Creating GUI theme")
    public GuiThemeVO createGuiTheme(CreateGuiThemeCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        String css = cmd.getCss();
        String jsonConfiguration = cmd.getJsonConfiguration();
        String commonNames = cmd.getCommonNames();
        String providedDomainIds = cmd.getDomainIds();
        String providedAccountIds = cmd.getAccountIds();
        boolean isPublic = cmd.getPublic();

        if (StringUtils.isAllBlank(css, jsonConfiguration)) {
            throw new CloudRuntimeException("Either the `css` or `jsonConfiguration` parameter must be informed.");
        }

        validateParameters(jsonConfiguration, providedDomainIds, providedAccountIds, commonNames, null);

        if (shouldSetGuiThemeToPrivate(providedDomainIds, providedAccountIds)) {
            isPublic = false;
        }

        GuiThemeVO guiThemeVO = new GuiThemeVO(name, description, css, jsonConfiguration, commonNames, providedDomainIds, providedAccountIds, isPublic, new Date(), null);
        return guiThemeDao.persist(guiThemeVO);
    }

    protected boolean shouldSetGuiThemeToPrivate(String providedDomainIds, String providedAccountIds) {
        if (StringUtils.isNotBlank(providedAccountIds)) {
            logger.info("Parameter `accountIds` was informed during GUI theme creation, therefore, `isPublic` will be set to `false`.");
            return true;
        }

        if (StringUtils.isNotBlank(providedDomainIds)) {
            logger.info("Parameter `domainIds` was informed during GUI theme creation, therefore, `isPublic` will be set to `false`.");
            return true;
        }
        return false;
    }

    /**
     * A GUI theme is only considered the default one if the parameters `commonNames`, `domainIds` and `accountIds` are all blank.
     * @return true if all parameters are blank, false otherwise.
     */
    protected boolean isConsideredDefaultTheme(String commonNames, String providedDomainIds, String providedAccountIds) {
        return StringUtils.isAllBlank(commonNames, providedDomainIds, providedAccountIds);
    }

    /**
     * There can only be one default theme registered, therefore, a {@link CloudRuntimeException} will be thrown if:
     * <ul>
     *     <li>There is already a default theme registered when creating a new GUI theme.</li>
     *     <li>Or, the GUI theme to be updated is not the default theme already registered.</li>
     * </ul>
     */
    protected void checkIfDefaultThemeIsAllowed(String commonNames, String providedDomainIds, String providedAccountIds, Long idOfThemeToBeUpdated) {
        if (!isConsideredDefaultTheme(commonNames, providedDomainIds, providedAccountIds)) {
            logger.info("The GUI theme will not be considered as the default one, as the `commonNames`, `domainIds` and `accountIds` are not all blank.");
            return;
        }

        GuiThemeVO defaultTheme = guiThemeDao.findDefaultTheme();

        if (defaultTheme != null && (idOfThemeToBeUpdated == null || defaultTheme.getId() != idOfThemeToBeUpdated.longValue())) {
            throw new CloudRuntimeException(String.format("Only one default GUI theme is allowed. Remove the current default theme %s and try again.", defaultTheme));
        }

        logger.info("The parameters `commonNames`, `domainIds` and `accountIds` were not informed. The created theme will be considered as the default theme.");
    }

    protected Pair<List<GuiThemeVO>, Integer> listGuiThemesWithNoAuthentication(ListGuiThemesCmd cmd) {
        return guiThemeDao.listGuiThemesWithNoAuthentication(cmd.getCommonName());
    }


    protected Pair<List<GuiThemeVO>, Integer> listGuiThemesInternal(ListGuiThemesCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        String commonName = cmd.getCommonName();
        String domainUuid = cmd.getDomainId() == null ? null : domainDao.findById(cmd.getDomainId()).getUuid();
        String accountUuid = cmd.getAccountId() == null ? null : accountDao.findById(cmd.getAccountId()).getUuid();
        boolean listAll = cmd.getListAll();
        boolean showRemoved = cmd.getShowRemoved();
        Boolean showPublic = cmd.getShowPublic();

        return guiThemeDao.listGuiThemes(id, name, commonName, domainUuid, accountUuid, listAll, showRemoved, showPublic);
    }

    protected void validateParameters(String jsonConfig, String domainIds, String accountIds, String commonNames, Long idOfThemeToBeUpdated) {
        if (isConsideredDefaultTheme(commonNames, domainIds, accountIds)) {
            checkIfDefaultThemeIsAllowed(commonNames, domainIds, accountIds, idOfThemeToBeUpdated);
        }

        validateObjectUuids(accountIds, Account.class);
        validateObjectUuids(domainIds, Domain.class);
        validateJsonConfiguration(jsonConfig);
    }

    protected void validateJsonConfiguration(String jsonConfig) {
        if (jsonConfig == null) {
            return;
        }

        JsonObject jsonObject = new JsonObject();

        try {
            JsonElement jsonElement = new JsonParser().parse(jsonConfig);
            Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
            entries.stream().forEach(entry -> validateJsonAttributes(entry, jsonObject));
        } catch (JsonSyntaxException exception) {
            logger.error(String.format("The following exception was thrown while parsing the JSON object: [%s].", exception.getMessage()));
            throw new CloudRuntimeException("Specified JSON configuration is not a valid JSON object.");
        }
    }

    /**
     * Validates the informed JSON attributes considering the allowed properties by the API, any invalid option is ignored.
     * All valid options are added to a {@link JsonObject} that will be considered as the final JSON configuration used by the GUI theme.
     */
    private void validateJsonAttributes(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        JsonElement entryValue = entry.getValue();
        String entryKey = entry.getKey();

        if (entryValue.isJsonPrimitive() && ALLOWED_PRIMITIVE_PROPERTIES.contains(entryKey)) {
            logger.trace("The JSON attribute [%s] is a valid option.");
            jsonObject.add(entryKey, entryValue);
        } else if (entryValue.isJsonObject() && ERROR.equals(entryKey)) {
            validateErrorAttribute(entry, jsonObject);
        } else if (entryValue.isJsonArray() && PLUGINS.equals(entryKey)) {
            validatePluginsAttribute(entry, jsonObject);
        } else {
            warnOfInvalidJsonAttribute(entryKey);
        }
    }

    /**
     * Creates a {@link JsonObject} with only the valid options for the Plugins' properties specified in the {@link #ALLOWED_PLUGIN_PROPERTIES}.
     */
    protected void validatePluginsAttribute(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        Set<Map.Entry<String, JsonElement>> entries = entry.getValue().getAsJsonArray().get(0).getAsJsonObject().entrySet();
        JsonObject objectToBeAdded = createJsonObject(entries, ALLOWED_PLUGIN_PROPERTIES);
        JsonArray jsonArray = new JsonArray();

        if (objectToBeAdded.entrySet().isEmpty()) {
            return;
        }

        jsonArray.add(objectToBeAdded);
        jsonObject.add(entry.getKey(), jsonArray);
    }

    /**
     * Creates a {@link JsonObject} with only the valid options for the Error's properties specified in the {@link #ALLOWED_ERROR_PROPERTIES}.
     */
    protected void validateErrorAttribute(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        Set<Map.Entry<String, JsonElement>> entries = entry.getValue().getAsJsonObject().entrySet();
        JsonObject objectToBeAdded = createJsonObject(entries, ALLOWED_ERROR_PROPERTIES);

        if (objectToBeAdded.entrySet().isEmpty()) {
            return;
        }

        jsonObject.add(entry.getKey(), objectToBeAdded);
    }

    protected JsonObject createJsonObject(Set<Map.Entry<String, JsonElement>> entries, List<String> allowedProperties) {
        JsonObject objectToBeAdded = new JsonObject();

        for (Map.Entry<String, JsonElement> recursiveEntry : entries) {
            String entryKey = recursiveEntry.getKey();

            if (!allowedProperties.contains(entryKey)) {
                warnOfInvalidJsonAttribute(entryKey);
                continue;
            }
            objectToBeAdded.add(entryKey, recursiveEntry.getValue());
        }

        return objectToBeAdded;
    }

    protected void warnOfInvalidJsonAttribute(String entryKey) {
        logger.warn(String.format("The JSON attribute [%s] is not a valid option, therefore, it will be ignored.", entryKey));
    }

    /**
     * Validate if the comma separated list of UUIDs of the fields {@link GuiThemeVO#accountUuids} and {@link GuiThemeVO#domainUuids} are valid.
     * @param providedIds a comma separated list of UUIDs of {@link Account} or {@link Domain}
     * @param clazz the class to infer the DAO object. Valid options are: {@link Account} and {@link Domain}
     */
    protected void validateObjectUuids(String providedIds, Class clazz) {
        if (StringUtils.isBlank(providedIds)) {
            return;
        }

        List<String> commaSeparatedIds = new ArrayList<>(Arrays.asList(providedIds.split("\\s*,\\s*")));
        for (String id : commaSeparatedIds) {
            Object objectVO = entityManager.findByUuid(clazz, id);

            if (objectVO == null) {
                throw new CloudRuntimeException(String.format("The %s ID %s does not exist. Verify the informed IDs and try again.", clazz.getSimpleName(), id));
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_UPDATE, eventDescription = "Updating GUI theme")
    public GuiThemeVO updateGuiTheme(UpdateGuiThemeCmd cmd) {
        Long guiThemeId = cmd.getId();
        GuiThemeVO guiThemeVO = guiThemeDao.findById(guiThemeId);

        String name = cmd.getName();
        String description = cmd.getDescription();
        String css = cmd.getCss();
        String jsonConfiguration = cmd.getJsonConfiguration();
        String commonNames = cmd.getCommonNames() == null ? guiThemeVO.getCommonNames() : cmd.getCommonNames();
        String providedDomainIds = cmd.getDomainIds() == null ? guiThemeVO.getDomainUuids() : cmd.getDomainIds();
        String providedAccountIds = cmd.getAccountIds() == null ? guiThemeVO.getAccountUuids() : cmd.getAccountIds();
        Boolean isPublic = cmd.getIsPublic();

        validateParameters(jsonConfiguration, providedDomainIds, providedAccountIds, commonNames, guiThemeId);

        if (shouldSetGuiThemeToPrivate(providedDomainIds, providedAccountIds)) {
            isPublic = false;
        }

        return persistGuiTheme(name, description, css, jsonConfiguration, commonNames, providedDomainIds, providedAccountIds, isPublic, guiThemeVO);

    }

    protected GuiThemeVO persistGuiTheme(String name, String description, String css, String jsonConfiguration, String commonNames, String providedDomainIds,
                                         String providedAccountIds, Boolean isPublic, GuiThemeVO guiThemeVO){
        if (name != null) {
            guiThemeVO.setName(ifBlankReturnNull(name));
        }

        if (description != null) {
            guiThemeVO.setDescription(ifBlankReturnNull(description));
        }

        if (css != null) {
            guiThemeVO.setCss(css);
        }

        if (jsonConfiguration != null) {
            guiThemeVO.setJsonConfiguration(jsonConfiguration);
        }

        if (commonNames != null) {
            guiThemeVO.setCommonNames(ifBlankReturnNull(commonNames));
        }

        if (providedAccountIds != null) {
            guiThemeVO.setAccountUuids(ifBlankReturnNull(providedAccountIds));
        }

        if (providedDomainIds != null) {
            guiThemeVO.setDomainUuids(ifBlankReturnNull(providedDomainIds));
        }

        if (isPublic != null) {
            guiThemeVO.setIsPublic(isPublic);
        }

        return guiThemeDao.persist(guiThemeVO);
    }

    protected String ifBlankReturnNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_GUI_THEME_REMOVE, eventDescription = "Removing GUI theme")
    public void removeGuiTheme(RemoveGuiThemeCmd cmd) {
        Long guiThemeId = cmd.getId();
        GuiThemeVO guiThemeVO = guiThemeDao.findById(guiThemeId);

        if (guiThemeVO != null) {
            guiThemeDao.remove(guiThemeId);
        } else {
            throw new CloudRuntimeException(String.format("Unable to find a GUI theme with the specified UUID."));
        }
    }
}
