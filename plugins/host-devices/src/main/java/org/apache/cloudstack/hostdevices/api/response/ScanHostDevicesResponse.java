package org.apache.cloudstack.hostdevices.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class ScanHostDevicesResponse extends BaseResponse {
    @SerializedName("success")
    @Param(description = "Indicates if the operation was successful")
    boolean success = true;
}
