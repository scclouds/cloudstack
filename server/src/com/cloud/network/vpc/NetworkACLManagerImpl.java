// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.vpc;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.util.List;


@Component
@Local(value = { NetworkACLManager.class})
public class NetworkACLManagerImpl extends ManagerBase implements NetworkACLManager{
    private static final Logger s_logger = Logger.getLogger(NetworkACLManagerImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    List<NetworkACLServiceProvider> _networkAclElements;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkDao _networkDao;

    @Override
    public NetworkACL createNetworkACL(String name, String description, long vpcId) {
        NetworkACLVO acl = new NetworkACLVO(name, description, vpcId);
        _networkACLDao.persist(acl);
        return acl;
    }

    @Override
    public boolean applyNetworkACL(long aclId) throws ResourceUnavailableException {
        boolean handled = false;
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(aclId);
        //Find all networks using this ACL
        List<NetworkVO> networks = _networkDao.listByAclId(aclId);
        for(NetworkVO network : networks){
            //Failure case??
            handled = applyACLItemsToNetwork(network.getId(), rules);
        }
        if(handled){
            for (NetworkACLItem rule : rules) {
                if (rule.getState() == NetworkACLItem.State.Revoke) {
                    removeRule(rule);
                } else if (rule.getState() == NetworkACLItem.State.Add) {
                    NetworkACLItemVO ruleVO = _networkACLItemDao.findById(rule.getId());
                    ruleVO.setState(NetworkACLItem.State.Active);
                    _networkACLItemDao.update(ruleVO.getId(), ruleVO);
                }
            }
        }
        return handled;
    }

    @Override
    public NetworkACL getNetworkACL(long id) {
        return _networkACLDao.findById(id);
    }

    @Override
    public boolean deleteNetworkACL(NetworkACL acl) {
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(acl.getId());
        if(aclItems.size() > 0){
            throw new CloudRuntimeException("ACL is not empty. Cannot delete network ACL: "+acl.getUuid());
        }
        return _networkACLDao.remove(acl.getId());
    }

    @Override
    public boolean replaceNetworkACL(NetworkACL acl, NetworkVO network) throws ResourceUnavailableException {
        if(network.getVpcId() != acl.getVpcId()){
            throw new InvalidParameterValueException("Network: "+network.getUuid()+" and ACL: "+acl.getUuid()+" do not belong to the same VPC");
        }
        network.setNetworkACLId(acl.getId());
        if(_networkDao.update(network.getId(), network)){
            return applyACLToNetwork(network.getId());
        }
        return false;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_ITEM_CREATE, eventDescription = "creating network ACL Item", create = true)
    public NetworkACLItem createNetworkACLItem(Integer portStart, Integer portEnd, String protocol, List<String> sourceCidrList,
                                                  Integer icmpCode, Integer icmpType, NetworkACLItem.TrafficType trafficType, Long aclId,
                                                  String action, Integer number) {
        NetworkACLItem.Action ruleAction = NetworkACLItem.Action.Allow;
        if("deny".equals(action)){
            ruleAction = NetworkACLItem.Action.Deny;
        }
        // If number is null, set it to currentMax + 1
        if(number == null){
            number = _networkACLItemDao.getMaxNumberByACL(aclId) + 1;
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        NetworkACLItemVO newRule = new NetworkACLItemVO(portStart, portEnd, protocol.toLowerCase(), aclId, sourceCidrList, icmpCode, icmpType, trafficType, ruleAction, number);
        newRule = _networkACLItemDao.persist(newRule);

        //ToDo: Is this required now with number??
        //detectNetworkACLConflict(newRule);

        if (!_networkACLItemDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("ACL Item Id: " + newRule.getId());

        txn.commit();

        return getNetworkACLItem(newRule.getId());
    }

    @Override
    public NetworkACLItem getNetworkACLItem(long ruleId) {
        return _networkACLItemDao.findById(ruleId);
    }

    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_ACL_DELETE, eventDescription = "revoking network acl", async = true)
    public boolean revokeNetworkACLItem(long ruleId) {

        NetworkACLItemVO rule = _networkACLItemDao.findById(ruleId);

        revokeRule(rule);

        boolean success = false;

        try {
            applyNetworkACL(rule.getAclId());
            success = true;
        } catch (ResourceUnavailableException e) {
            return false;
        }

        return success;
    }

    @DB
    private void revokeRule(NetworkACLItemVO rule) {
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _networkACLItemDao.remove(rule.getId());
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _networkACLItemDao.update(rule.getId(), rule);
        }
    }

    @Override
    public boolean revokeACLItemsForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        List<NetworkACLItemVO> aclItems = _networkACLItemDao.listByACL(network.getNetworkACLId());
        if (aclItems.isEmpty()) {
            s_logger.debug("Found no network ACL Items for network id=" + networkId);
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + aclItems.size() + " Network ACL Items for network id=" + networkId);
        }

        for (NetworkACLItemVO aclItem : aclItems) {
            // Mark all Network ACLs rules as Revoke, but don't update in DB
            if (aclItem.getState() == State.Add || aclItem.getState() == State.Active) {
                aclItem.setState(State.Revoke);
            }
        }

        boolean success = applyACLItemsToNetwork(network.getId(), aclItems);

        if (s_logger.isDebugEnabled() && success) {
            s_logger.debug("Successfully released Network ACLs for network id=" + networkId + " and # of rules now = "
                    + aclItems.size());
        }

        return success;
    }

    @Override
    public List<NetworkACLItemVO> listNetworkACLItems(long guestNtwkId) {
        Network network = _networkMgr.getNetwork(guestNtwkId);
        return _networkACLItemDao.listByACL(network.getNetworkACLId());
    }

    private void removeRule(NetworkACLItem rule) {
        //remove the rule
        _networkACLItemDao.remove(rule.getId());
    }

    @Override
    public boolean applyACLToNetwork(long networkId) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(network.getNetworkACLId());
        return applyACLItemsToNetwork(networkId, rules);
    }

    public boolean applyACLItemsToNetwork(long networkId, List<NetworkACLItemVO> rules) throws ResourceUnavailableException {
        Network network = _networkDao.findById(networkId);
        boolean handled = false;
        for (NetworkACLServiceProvider element: _networkAclElements) {
            Network.Provider provider = element.getProvider();
            boolean  isAclProvider = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.NetworkACL, provider);
            if (!isAclProvider) {
                continue;
            }
            handled = element.applyNetworkACLs(network, rules);
            if (handled)
                break;
        }
        return handled;
    }

}
