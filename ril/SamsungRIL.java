/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2011-2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import java.util.ArrayList;
import java.util.Collections;
import java.lang.Runtime;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.os.Parcel;
import android.os.Registrant;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import static com.android.internal.telephony.RILConstants.*;

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaSignalInfoRec;
import com.android.internal.telephony.cdma.SignalToneUtil;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.dataconnection.DcFailCause;

import android.telephony.Rlog;

public class SamsungRIL extends RIL implements CommandsInterface {

    private boolean mSignalbarCount = SystemProperties.getInt("ro.telephony.sends_barcount", 0) == 1 ? true : false;
    private boolean mIsSamsungCdma = SystemProperties.getBoolean("ro.ril.samsung_cdma", false);
    private Object mCatProCmdBuffer;

    public SamsungRIL(Context context, int networkMode, int cdmaSubscription) {
        super(context, networkMode, cdmaSubscription);
    }

    // SAMSUNG SGS STATES
    static final int RIL_UNSOL_SAMSUNG_UNKNOWN_MAGIC_REQUEST = 11012;
    static final int RIL_UNSOL_SAMSUNG_UNKNOWN_MAGIC_REQUEST_2 = 11011;
    static final int RIL_REQUEST_DIAL_EMERGENCY = 10016;
    
    static final int BRIL_HOOK_PBK_READ_ENTRY_REQ = 10;
    static final int BRIL_HOOK_PBK_SEND_INFO_REQ = 9;
    static final int BRIL_HOOK_PBK_UPDATE_ENTRY_REQ = 11;
    static final int BRIL_HOOK_POWER_ONOFF_CARD = 8;
    static final int BRIL_HOOK_QUERY_SIM_PIN_REMAINING = 1;
    static final int BRIL_HOOK_SET_BPM_MODE = 3;
    static final int BRIL_HOOK_SET_FAST_DORMANCY = 12;
    static final int BRIL_HOOK_SET_PREFDATA = 0;
    static final int CDMA_CELL_BROADCAST_SMS_DISABLED = 1;
    static final int CDMA_CELL_BROADCAST_SMS_ENABLED = 0;
    static final int CDMA_PHONE = 2;
    static final int CDM_TTY_FULL_MODE = 1;
    static final int CDM_TTY_HCO_MODE = 2;
    static final int CDM_TTY_MODE_DISABLED = 0;
    static final int CDM_TTY_MODE_ENABLED = 1;
    static final int CDM_TTY_VCO_MODE = 3;
    static final int DATA_PROFILE_BIP = 1003;
    static final int DATA_PROFILE_CAS = 1004;
    static final int DATA_PROFILE_CBS = 4;
    static final int DATA_PROFILE_CMDM = 9;
    static final int DATA_PROFILE_CMMAIL = 10;
    static final int DATA_PROFILE_DEFAULT = 0;
    static final int DATA_PROFILE_DM = 8;
    static final int DATA_PROFILE_E911 = 1001;
    static final int DATA_PROFILE_EMBMS = 1002;
    static final int DATA_PROFILE_FOTA = 3;
    static final int DATA_PROFILE_HIPRI = 7;
    static final int DATA_PROFILE_IMS = 2;
    static final int DATA_PROFILE_MMS = 5;
    static final int DATA_PROFILE_OEM_BASE = 1000;
    static final int DATA_PROFILE_SUPL = 6;
    static final int DATA_PROFILE_TETHERED = 1;
    static final int DATA_PROFILE_WAP = 11;
    static final int DEACTIVATE_REASON_NONE = 0;
    static final int DEACTIVATE_REASON_PDP_RESET = 2;
    static final int DEACTIVATE_REASON_RADIO_OFF = 1;
    static final int DIAL_MODIFIED_TO_DIAL = 19;
    static final int DIAL_MODIFIED_TO_SS = 18;
    static final int DIAL_MODIFIED_TO_USSD = 17;
    static final int DIAL_STR_TOO_LONG = 1004;
    static final int FDN_CHECK_FAILURE = 14;
    static final int GENERIC_FAILURE = 2;
    static final int GSM_PHONE = 1;
    static final int ILLEGAL_SIM_OR_ME = 15;
    static final int IMS_PHONE = 5;
    static final int INVALID_CHARACTERS_IN_DIAL_STR = 1006;
    static final int INVALID_CHARACTERS_IN_TEXT_STR = 1005;
    static final int INVALID_INDEX = 1002;
    static final int INVALID_PARAMETER = 31;
    static final int LTE_ON_CDMA_FALSE = 0;
    static final int LTE_ON_CDMA_TRUE = 1;
    static final int LTE_ON_CDMA_UNKNOWN = -1;
    static final int MAX_INT = 2147483647;
    static final int MEMORY_ERROR = 1001;
    static final int MISSING_RESOURCE = 29;
    static final int MODE_NOT_SUPPORTED = 13;
    static final int NETWORK_MODE_CDMA = 4;
    static final int NETWORK_MODE_CDMA_NO_EVDO = 5;
    static final int NETWORK_MODE_EVDO_NO_CDMA = 6;
    static final int NETWORK_MODE_GLOBAL = 7;
    static final int NETWORK_MODE_GSM_ONLY = 1;
    static final int NETWORK_MODE_GSM_UMTS = 3;
    static final int NETWORK_MODE_LTE_CDMA_EVDO = 8;
    static final int NETWORK_MODE_LTE_CMDA_EVDO_GSM_WCDMA = 10;
    static final int NETWORK_MODE_LTE_GSM_WCDMA = 9;
    static final int NETWORK_MODE_LTE_ONLY = 11;
    static final int NETWORK_MODE_LTE_WCDMA = 12;
    static final int NETWORK_MODE_TDSCDMA_PREF = 23;
    static final int NETWORK_MODE_TD_SCDMA_CDMA_EVDO_GSM_WCDMA = 21;
    static final int NETWORK_MODE_TD_SCDMA_GSM = 16;
    static final int NETWORK_MODE_TD_SCDMA_GSM_LTE = 17;
    static final int NETWORK_MODE_TD_SCDMA_GSM_WCDMA = 18;
    static final int NETWORK_MODE_TD_SCDMA_GSM_WCDMA_LTE = 20;
    static final int NETWORK_MODE_TD_SCDMA_LTE = 15;
    static final int NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA = 22;
    static final int NETWORK_MODE_TD_SCDMA_ONLY = 13;
    static final int NETWORK_MODE_TD_SCDMA_WCDMA = 14;
    static final int NETWORK_MODE_TD_SCDMA_WCDMA_LTE = 19;
    static final int NETWORK_MODE_WCDMA_ONLY = 2;
    static final int NETWORK_MODE_WCDMA_PREF = 0;
    static final int NOT_SUBCRIBED_USER = 28;
    static final int NO_PHONE = 0;
    static final int NO_SUCH_ELEMENT = 30;
    static final int OPER_NOT_ALLOWED = 1000;
    static final int OP_NOT_ALLOWED_BEFORE_REG_NW = 9;
    static final int OP_NOT_ALLOWED_DURING_VOICE_CALL = 8;
    static final int PASSWORD_INCORRECT = 3;
    static final int PREFERRED_NETWORK_MODE = 0;
    static final int RADIO_NOT_AVAILABLE = 1;
    static final int REQUEST_CANCELLED = 7;
    static final int REQUEST_NOT_SUPPORTED = 6;
    static final int RIL_CHN_CDMA_REQUEST_BASE = 10057;
    static final int RIL_CHN_REQUEST_BASE = 10053;
    static final int RIL_CHN_REQUEST_LAST = 10057;
    static final int RIL_ERRNO_INVALID_RESPONSE = -1;
    static final int RIL_KOR_REQUEST_BASE = 10036;
    static final int RIL_KOR_REQUEST_LAST = 10040;
    static final int RIL_KOR_UNSOL_BASE = 11036;
    static final int RIL_KOR_UNSOL_LAST = 11040;
    static final int RIL_LTE_REQUEST_BASE = 10033;
    static final int RIL_LTE_REQUEST_LAST = 10036;
    static final int RIL_LTE_UNSOL_BASE = 11033;
    static final int RIL_LTE_UNSOL_LAST = 11036;
    static final int RIL_OEM_REQUEST_BASE = 10000;
    static final int RIL_OEM_UNSOL_LAST = 11033;
    static final int RIL_OEM_UNSOL_RESPONSE_BASE = 11000;
    static final int RIL_REQUEST_ACCESS_PHONEBOOK_ENTRY = 10009;
    static final int RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU = 106;
    static final int RIL_REQUEST_ACTIVATE_DATA_CALL = 10036;
    static final int RIL_REQUEST_ANSWER = 40;
    static final int RIL_REQUEST_BASEBAND_VERSION = 51;
    static final int RIL_REQUEST_CALL_DEFLECTION = 10011;
    static final int RIL_REQUEST_CANCEL_USSD = 30;
    static final int RIL_REQUEST_CDMA_BROADCAST_ACTIVATION = 94;
    static final int RIL_REQUEST_CDMA_BURST_DTMF = 85;
    static final int RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM = 97;
    static final int RIL_REQUEST_CDMA_FLASH = 84;
    static final int RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG = 92;
    static final int RIL_REQUEST_CDMA_GET_DATAPROFILE = 10045;
    static final int RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE = 104;
    static final int RIL_REQUEST_CDMA_GET_SYSTEMPROPERTIES = 10047;
    static final int RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE = 83;
    static final int RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE = 79;
    static final int RIL_REQUEST_CDMA_SEND_SMS = 87;
    static final int RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE = 10040;
    static final int RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG = 93;
    static final int RIL_REQUEST_CDMA_SET_DATAPROFILE = 10046;
    static final int RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE = 82;
    static final int RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE = 78;
    static final int RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE = 77;
    static final int RIL_REQUEST_CDMA_SET_SYSTEMPROPERTIES = 10048;
    static final int RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE = 88;
    static final int RIL_REQUEST_CDMA_SUBSCRIPTION = 95;
    static final int RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY = 86;
    static final int RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM = 96;
    static final int RIL_REQUEST_CHANGE_BARRING_PASSWORD = 44;
    static final int RIL_REQUEST_CHANGE_SIM_PERSO = 10037;
    static final int RIL_REQUEST_CHANGE_SIM_PIN = 6;
    static final int RIL_REQUEST_CHANGE_SIM_PIN2 = 7;
    static final int RIL_REQUEST_CONFERENCE = 16;
    static final int RIL_REQUEST_DATA_CALL_LIST = 57;
    static final int RIL_REQUEST_DATA_REGISTRATION_STATE = 21;
    static final int RIL_REQUEST_DEACTIVATE_DATA_CALL = 41;
    static final int RIL_REQUEST_DELETE_SMS_ON_SIM = 64;
    static final int RIL_REQUEST_DEVICE_IDENTITY = 98;
    static final int RIL_REQUEST_DIAL = 10;
    static final int RIL_REQUEST_DIAL_EMERGENCY_CALL = 10016;
    static final int RIL_REQUEST_DIAL_VIDEO_CALL = 10010;
    static final int RIL_REQUEST_DIAL_VT = 500;
    static final int RIL_REQUEST_DISCON_DUN = 10059;
    static final int RIL_REQUEST_DTMF = 24;
    static final int RIL_REQUEST_DTMF_START = 49;
    static final int RIL_REQUEST_DTMF_STOP = 50;
    static final int RIL_REQUEST_ENTER_DEPERSONALIZATION_CODE = 8;
    static final int RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION = 8;
    static final int RIL_REQUEST_ENTER_SIM_PERSO = 10038;
    static final int RIL_REQUEST_ENTER_SIM_PIN = 2;
    static final int RIL_REQUEST_ENTER_SIM_PIN2 = 4;
    static final int RIL_REQUEST_ENTER_SIM_PUK = 3;
    static final int RIL_REQUEST_ENTER_SIM_PUK2 = 5;
    static final int RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE = 99;
    static final int RIL_REQUEST_EXPLICIT_CALL_TRANSFER = 72;
    static final int RIL_REQUEST_EXT_BASE = 500;
    static final int RIL_REQUEST_FAST_DORMANCY = 508;
    static final int RIL_REQUEST_GET_ACM = 503;
    static final int RIL_REQUEST_GET_AMM = 505;
    static final int RIL_REQUEST_GET_BAND = 510;
    static final int RIL_REQUEST_GET_BARCODE_NUMBER = 10023;
    static final int RIL_REQUEST_GET_CELL_BROADCAST_CONFIG = 10002;
    static final int RIL_REQUEST_GET_CELL_INFO_LIST = 109;
    static final int RIL_REQUEST_GET_CLIR = 31;
    static final int RIL_REQUEST_GET_CNAP = 515;
    static final int RIL_REQUEST_GET_CPUC = 507;
    static final int RIL_REQUEST_GET_CURRENT_CALLS = 9;
    static final int RIL_REQUEST_GET_DATA_CALL_PROFILE = 114;
    static final int RIL_REQUEST_GET_DATA_SUBSCRIPTION = 118;
    static final int RIL_REQUEST_GET_IMEI = 38;
    static final int RIL_REQUEST_GET_IMEISV = 39;
    static final int RIL_REQUEST_GET_IMSI = 11;
    static final int RIL_REQUEST_GET_LINE_ID = 10019;
    static final int RIL_REQUEST_GET_MANUFACTURE_DATE_NUMBER = 10022;
    static final int RIL_REQUEST_GET_MUTE = 54;
    static final int RIL_REQUEST_GET_NEIGHBORING_CELL_IDS = 75;
    static final int RIL_REQUEST_GET_PHONEBOOK_ENTRY = 10008;
    static final int RIL_REQUEST_GET_PHONEBOOK_STORAGE_INFO = 10007;
    static final int RIL_REQUEST_GET_PREFERRED_NETWORK_LIST = 10055;
    static final int RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE = 74;
    static final int RIL_REQUEST_GET_QOS_STATUS = 122;
    static final int RIL_REQUEST_GET_SERIAL_NUMBER = 10021;
    static final int RIL_REQUEST_GET_SIM_STATUS = 1;
    static final int RIL_REQUEST_GET_SMSC_ADDRESS = 100;
    static final int RIL_REQUEST_GET_TIME_INFO = 10039;
    static final int RIL_REQUEST_GET_UICC_SUBSCRIPTION = 117;
    static final int RIL_REQUEST_GSM_BROADCAST_ACTIVATION = 91;
    static final int RIL_REQUEST_GSM_GET_BROADCAST_CONFIG = 89;
    static final int RIL_REQUEST_GSM_SET_BROADCAST_CONFIG = 90;
    static final int RIL_REQUEST_HANGUP = 12;
    static final int RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND = 14;
    static final int RIL_REQUEST_HANGUP_VT = 10056;
    static final int RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND = 13;
    static final int RIL_REQUEST_HOLD = 10057;
    static final int RIL_REQUEST_IMS_REGISTRATION_STATE = 112;
    static final int RIL_REQUEST_IMS_SEND_SMS = 113;
    static final int RIL_REQUEST_ISIM_AUTHENTICATION = 105;
    static final int RIL_REQUEST_LAST = 10033;
    static final int RIL_REQUEST_LAST_CALL_FAIL_CAUSE = 18;
    static final int RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE = 56;
    static final int RIL_REQUEST_LOCK_INFO = 10014;
    static final int RIL_REQUEST_MODEM_HANGUP = 10052;
    static final int RIL_REQUEST_MODIFY_CALL_CONFIRM = 10032;
    static final int RIL_REQUEST_MODIFY_CALL_INITIATE = 10031;
    static final int RIL_REQUEST_MODIFY_QOS = 123;
    static final int RIL_REQUEST_OEM_BASE = 112;
    static final int RIL_REQUEST_OEM_HOOK_RAW = 59;
    static final int RIL_REQUEST_OEM_HOOK_STRINGS = 60;
    static final int RIL_REQUEST_OMADM_CLIENT_START_SESSION = 10043;
    static final int RIL_REQUEST_OMADM_SEND_DATA = 10044;
    static final int RIL_REQUEST_OMADM_SERVER_START_SESSION = 10042;
    static final int RIL_REQUEST_OMADM_SETUP_SESSION = 10041;
    static final int RIL_REQUEST_OPERATOR = 22;
    static final int RIL_REQUEST_PS_ATTACH = 10034;
    static final int RIL_REQUEST_PS_DETACH = 10035;
    static final int RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE = 66;
    static final int RIL_REQUEST_QUERY_AVAILABLE_NETWORKS = 48;
    static final int RIL_REQUEST_QUERY_CALL_FORWARD_STATUS = 33;
    static final int RIL_REQUEST_QUERY_CALL_WAITING = 35;
    static final int RIL_REQUEST_QUERY_CLIP = 55;
    static final int RIL_REQUEST_QUERY_COLP = 512;
    static final int RIL_REQUEST_QUERY_COLR = 517;
    static final int RIL_REQUEST_QUERY_FACILITY_LOCK = 42;
    static final int RIL_REQUEST_QUERY_LOCK_NETWORKS = 10058;
    static final int RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE = 45;
    static final int RIL_REQUEST_QUERY_TTY_MODE = 81;
    static final int RIL_REQUEST_RADIO_POWER = 23;
    static final int RIL_REQUEST_RELEASE_QOS = 121;
    static final int RIL_REQUEST_REPORT_SMS_MEMORY_STATUS = 102;
    static final int RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING = 103;
    static final int RIL_REQUEST_RESET_CP = 127;
    static final int RIL_REQUEST_RESET_RADIO = 58;
    static final int RIL_REQUEST_RESUME_QOS = 125;
    static final int RIL_REQUEST_SAFE_MODE = 10033;
    static final int RIL_REQUEST_SCREEN_STATE = 61;
    static final int RIL_REQUEST_SELECT_BAND = 509;
    static final int RIL_REQUEST_SEND_ENCODED_USSD = 10005;
    static final int RIL_REQUEST_SEND_SMS = 25;
    static final int RIL_REQUEST_SEND_SMS_COUNT = 10049;
    static final int RIL_REQUEST_SEND_SMS_EXPECT_MORE = 26;
    static final int RIL_REQUEST_SEND_SMS_MSG = 10050;
    static final int RIL_REQUEST_SEND_SMS_MSG_READ_STATUS = 10051;
    static final int RIL_REQUEST_SEND_USSD = 29;
    static final int RIL_REQUEST_SEPARATE_CONNECTION = 52;
    static final int RIL_REQUEST_SETUP_DATA_CALL = 27;
    static final int RIL_REQUEST_SETUP_QOS = 120;
    static final int RIL_REQUEST_SET_ACM = 502;
    static final int RIL_REQUEST_SET_AMM = 504;
    static final int RIL_REQUEST_SET_BAND_MODE = 65;
    static final int RIL_REQUEST_SET_CALL_FORWARD = 34;
    static final int RIL_REQUEST_SET_CALL_WAITING = 36;
    static final int RIL_REQUEST_SET_CLIP = 513;
    static final int RIL_REQUEST_SET_CLIR = 32;
    static final int RIL_REQUEST_SET_CNAP = 516;
    static final int RIL_REQUEST_SET_COLP = 514;
    static final int RIL_REQUEST_SET_COLR = 518;
    static final int RIL_REQUEST_SET_CPUC = 506;
    static final int RIL_REQUEST_SET_DATA_SUBSCRIPTION = 116;
    static final int RIL_REQUEST_SET_FACILITY_LOCK = 43;
    static final int RIL_REQUEST_SET_FDY = 524;
    static final int RIL_REQUEST_SET_INITIAL_ATTACH_APN = 111;
    static final int RIL_REQUEST_SET_LINE_ID = 10020;
    static final int RIL_REQUEST_SET_LOCATION_UPDATES = 76;
    static final int RIL_REQUEST_SET_MUTE = 53;
    static final int RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC = 46;
    static final int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL = 47;
    static final int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_EXT = 511;
    static final int RIL_REQUEST_SET_PDA_MEMORY_STATUS = 10006;
    static final int RIL_REQUEST_SET_PREFERRED_NETWORK_LIST = 10054;
    static final int RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE = 73;
    static final int RIL_REQUEST_SET_SIM_POWER = 10053;
    static final int RIL_REQUEST_SET_SMSC_ADDRESS = 101;
    static final int RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION = 62;
    static final int RIL_REQUEST_SET_TRANSMIT_POWER = 119;
    static final int RIL_REQUEST_SET_TTY_MODE = 80;
    static final int RIL_REQUEST_SET_UICC_SUBSCRIPTION = 115;
    static final int RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE = 110;
    static final int RIL_REQUEST_SIGNAL_STRENGTH = 19;
    static final int RIL_REQUEST_SIM_AUTH = 10030;
    static final int RIL_REQUEST_SIM_CLOSE_CHANNEL = 10028;
    static final int RIL_REQUEST_SIM_IO = 28;
    static final int RIL_REQUEST_SIM_OPEN_CHANNEL = 10027;
    static final int RIL_REQUEST_SIM_TRANSMIT_BASIC = 10026;
    static final int RIL_REQUEST_SIM_TRANSMIT_CHANNEL = 10029;
    static final int RIL_REQUEST_SMS_ACKNOWLEDGE = 37;
    static final int RIL_REQUEST_STK_GET_PROFILE = 67;
    static final int RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM = 71;
    static final int RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND = 69;
    static final int RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS = 107;
    static final int RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE = 70;
    static final int RIL_REQUEST_STK_SET_PROFILE = 68;
    static final int RIL_REQUEST_STK_SIM_INIT_EVENT = 10018;
    static final int RIL_REQUEST_SUSPEND_QOS = 124;
    static final int RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE = 15;
    static final int RIL_REQUEST_SYNC_AUDIO = 126;
    static final int RIL_REQUEST_UDUB = 17;
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_BOOTSTRAP = 10024;
    static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_NAF = 10025;
    static final int RIL_REQUEST_USIM_PB_CAPA = 10013;
    static final int RIL_REQUEST_VOICE_RADIO_TECH = 108;
    static final int RIL_REQUEST_VOICE_REGISTRATION_STATE = 20;
    static final int RIL_REQUEST_WRITE_SMS_TO_SIM = 63;
    static final int RIL_RESTRICTED_STATE_CS_ALL = 4;
    static final int RIL_RESTRICTED_STATE_CS_EMERGENCY = 1;
    static final int RIL_RESTRICTED_STATE_CS_NORMAL = 2;
    static final int RIL_RESTRICTED_STATE_NONE = 0;
    static final int RIL_RESTRICTED_STATE_PS_ALL = 16;
    static final int RIL_UNSOL_1X_SMSPP = 11023;
    static final int RIL_UNSOL_AM = 11010;
    static final int RIL_UNSOL_CALL_RING = 1018;
    static final int RIL_UNSOL_CDMA_CALL_WAITING = 1025;
    static final int RIL_UNSOL_CDMA_INFO_REC = 1027;
    static final int RIL_UNSOL_CDMA_OTA_PROVISION_STATUS = 1026;
    static final int RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL = 1022;
    static final int RIL_UNSOL_CDMA_SIP_REG_NOTI = 11040;
    static final int RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 1031;
    static final int RIL_UNSOL_CELL_INFO_LIST = 1036;
    static final int RIL_UNSOL_CP_ASSERTED_OR_RESETTING = 2147483647;
    static final int RIL_UNSOL_CS_FALLBACK = 11030;
    static final int RIL_UNSOL_DATA_CALL_LIST_CHANGED = 1010;
    static final int RIL_UNSOL_DATA_NETWORK_STATE_CHANGED = 1039;
    static final int RIL_UNSOL_DATA_SUSPEND_RESUME = 11012;
    static final int RIL_UNSOL_DEVICE_READY_NOTI = 11008;
    static final int RIL_UNSOL_DHA_STATE = 11019;
    static final int RIL_UNSOL_DUN = 11042;
    static final int RIL_UNSOL_DUN_CALL_STATUS = 11004;
    static final int RIL_UNSOL_DUN_INFO = 1043;
    static final int RIL_UNSOL_DUN_PIN_CONTROL_SIGNAL = 11011;
    static final int RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE = 1024;
    static final int RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE = 1033;
    static final int RIL_UNSOL_FACTORY_AM = 11026;
    static final int RIL_UNSOL_GPS_NOTI = 11009;
    static final int RIL_UNSOL_HOME_NETWORK_NOTI = 11039;
    static final int RIL_UNSOL_HSDPA_STATE_CHANGED = 11016;
    static final int RIL_UNSOL_IMS_REGISTRATION_STATE_CHANGED = 11027;
    static final int RIL_UNSOL_IPV6_ADDR = 11035;
    static final int RIL_UNSOL_MIP_CONNECT_STATUS = 11048;
    static final int RIL_UNSOL_MODIFY_CALL = 11028;
    static final int RIL_UNSOL_NITZ_TIME_RECEIVED = 1008;
    static final int RIL_UNSOL_NWK_INIT_DISC_REQUEST = 11036;
    static final int RIL_UNSOL_O2_HOME_ZONE_INFO = 11007;
    static final int RIL_UNSOL_OEM_HOOK_RAW = 1028;
    static final int RIL_UNSOL_OMADM_SEND_DATA = 11041;
    static final int RIL_UNSOL_ON_SS = 1040;
    static final int RIL_UNSOL_ON_USSD = 1006;
    static final int RIL_UNSOL_ON_USSD_REQUEST = 1007;
    static final int RIL_UNSOL_PCMCLOCK_STATE = 11022;
    static final int RIL_UNSOL_QOS_STATE_CHANGED_IND = 1042;
    static final int RIL_UNSOL_RELEASE_COMPLETE_MESSAGE = 11001;
    static final int RIL_UNSOL_RESEND_INCALL_MUTE = 1030;
    static final int RIL_UNSOL_RESPONSE_BASE = 1000;
    static final int RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED = 1001;
    static final int RIL_UNSOL_RESPONSE_CDMA_NEW_SMS = 1020;
    static final int RIL_UNSOL_RESPONSE_EXT_BASE = 1500;
    static final int RIL_UNSOL_RESPONSE_HANDOVER = 11034;
    static final int RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED = 1037;
    static final int RIL_UNSOL_RESPONSE_LINE_SMS_COUNT = 11005;
    static final int RIL_UNSOL_RESPONSE_LINE_SMS_READ = 11006;
    static final int RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS = 1021;
    static final int RIL_UNSOL_RESPONSE_NEW_CB_MSG = 11000;
    static final int RIL_UNSOL_RESPONSE_NEW_SMS = 1003;
    static final int RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM = 1005;
    static final int RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT = 1004;
    static final int RIL_UNSOL_RESPONSE_NO_NETWORK_RESPONSE = 11014;
    static final int RIL_UNSOL_RESPONSE_OEM_BASE = 1037;
    static final int RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED = 1000;
    static final int RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED = 1019;
    static final int RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED = 1002;
    static final int RIL_UNSOL_RESTRICTED_STATE_CHANGED = 1023;
    static final int RIL_UNSOL_RILD_RESET_NOTI = 11038;
    static final int RIL_UNSOL_RIL_CONNECTED = 1034;
    static final int RIL_UNSOL_RINGBACK_TONE = 1029;
    static final int RIL_UNSOL_RINGBACK_TONE_STATE = 11025;
    static final int RIL_UNSOL_RTS_INDICATION = 11037;
    static final int RIL_UNSOL_SAP = 11013;
    static final int RIL_UNSOL_SIGNAL_STRENGTH = 1009;
    static final int RIL_UNSOL_SIM_APPLICATION_REFRESH = 1100;
    static final int RIL_UNSOL_SIM_PB_READY = 11021;
    static final int RIL_UNSOL_SIM_REFRESH = 1017;
    static final int RIL_UNSOL_SIM_SMS_STORAGE_AVAILALE = 11015;
    static final int RIL_UNSOL_SIM_SMS_STORAGE_FULL = 1016;
    static final int RIL_UNSOL_SMARTAS_NOTI = 11033;
    static final int RIL_UNSOL_SRVCC_HANDOVER = 11029;
    static final int RIL_UNSOL_STK_CALL_CONTROL_RESULT = 11003;
    static final int RIL_UNSOL_STK_CALL_SETUP = 1015;
    static final int RIL_UNSOL_STK_CALL_SETUP_RESULT = 1501;
    static final int RIL_UNSOL_STK_CALL_SETUP_STATUS = 1500;
    static final int RIL_UNSOL_STK_CALL_STATUS = 11049;
    static final int RIL_UNSOL_STK_CC_ALPHA_NOTIFY = 1041;
    static final int RIL_UNSOL_STK_EVENT_NOTIFY = 1014;
    static final int RIL_UNSOL_STK_PROACTIVE_COMMAND = 1013;
    static final int RIL_UNSOL_STK_SEND_SMS_RESULT = 11002;
    static final int RIL_UNSOL_STK_SEND_SM_RESULT = 1503;
    static final int RIL_UNSOL_STK_SEND_SM_STATUS = 1502;
    static final int RIL_UNSOL_STK_SEND_USSD_RESULT = 1504;
    static final int RIL_UNSOL_STK_SESSION_END = 1012;
    static final int RIL_UNSOL_SUPP_SVC_NOTIFICATION = 1011;
    static final int RIL_UNSOL_SYSTEM_REBOOT = 11043;
    static final int RIL_UNSOL_TETHERED_MODE_STATE_CHANGED = 1038;
    static final int RIL_UNSOL_TWO_MIC_STATE = 11018;
    static final int RIL_UNSOL_UART = 11020;
    static final int RIL_UNSOL_UICC_APPLICATION_STATUS = 1101;
    static final int RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED = 11031;
    static final int RIL_UNSOL_UTS_GETSMSCOUNT = 11045;
    static final int RIL_UNSOL_UTS_GETSMSMSG = 11046;
    static final int RIL_UNSOL_UTS_GET_UNREAD_SMS_STATUS = 11047;
    static final int RIL_UNSOL_VE = 11024;
    static final int RIL_UNSOL_VOICE_PRIVACY_CHANGED = 11044;
    static final int RIL_UNSOL_VOICE_RADIO_TECH_CHANGED = 1035;
    static final int RIL_UNSOL_VOICE_SYSTEM_ID = 11032;
    static final int RIL_UNSOL_WB_AMR_STATE = 11017;
    static final int RIL_UNSOl_CDMA_PRL_CHANGED = 1032;
    static final int RIL_USA_CDMA_REQUEST_BASE = 10040;
    static final int RIL_USA_CDMA_REQUEST_LAST = 10053;
    static final int RIL_USA_CDMA_UNSOL_BASE = 11040;
    static final int RIL_USA_CDMA_UNSOL_LAST = 11049;
    static final int RIL_USA_GSM_REQUEST_BASE = 10053;
    static final int RIL_USA_GSM_REQUEST_LAST = 10053;
    static final int RIL_USA_GSM_UNSOL_BASE = 11049;
    static final int RIL_USA_GSM_UNSOL_LAST = 11049;
    static final int SEC_SIP_PHONE = 4;
    static final int SETUP_DATA_AUTH_CHAP = 2;
    static final int SETUP_DATA_AUTH_NONE = 0;
    static final int SETUP_DATA_AUTH_PAP = 1;
    static final int SETUP_DATA_AUTH_PAP_CHAP = 3;
    static final int SETUP_DATA_CALL_FAILURE = 16;
    public static final String SETUP_DATA_PROTOCOL_IP = "IP";
    public static final String SETUP_DATA_PROTOCOL_IPV4V6 = "IPV4V6";
    public static final String SETUP_DATA_PROTOCOL_IPV6 = "IPV6";
    static final int SETUP_DATA_TECH_CDMA = 0;
    static final int SETUP_DATA_TECH_GSM = 1;
    static final int SIM_ABSENT = 11;
    static final int SIM_PIN2 = 4;
    static final int SIM_PUK2 = 5;
    static final int SIP_PHONE = 3;
    static final int SMS_DSAC_FAILURE = 27;
    static final int SMS_SEND_FAIL_RETRY = 10;
    static final int SPECIAL_ERROR_CODE_BASE = -1000;
    static final int SS_MODIFIED_TO_DIAL = 23;
    static final int SS_MODIFIED_TO_SS = 25;
    static final int SS_MODIFIED_TO_USSD = 24;
    static final int SUBSCRIPTION_NOT_AVAILABLE = 12;
    static final int SUBSCRIPTION_NOT_SUPPORTED = 26;
    static final int SUCCESS = 0;
    static final int TEXT_STR_TOO_LONG = 1003;
    static final int TRANSMIT_POWER_DEFAULT = 0;
    static final int TRANSMIT_POWER_WIFI_HOTSPOT = 1;
    static final int USSD_MODIFIED_TO_DIAL = 20;
    static final int USSD_MODIFIED_TO_SS = 21;
    static final int USSD_MODIFIED_TO_USSD = 22;

    static String
    requestToString(int request) {
        switch (request) {
            case RIL_REQUEST_DIAL_EMERGENCY: return "DIAL_EMERGENCY";
            default: return RIL.requestToString(request);
        }
    }

    @Override
    public void
    setRadioPower(boolean on, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RADIO_POWER, result);

        if (on) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(1);
        } else {
            rr.mParcel.writeInt(2);
            rr.mParcel.writeInt(0);
            rr.mParcel.writeInt(0);
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    protected RILRequest
    processSolicited (Parcel p) {
        int serial, error;

        serial = p.readInt();
        error = p.readInt();

        Rlog.d(RILJ_LOG_TAG, "Serial: " + serial);
        Rlog.d(RILJ_LOG_TAG, "Error: " + error);

        RILRequest rr;

        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited response! sn: "
                    + serial + " error: " + error);
            return null;
        }

        Object ret = null;

        if (error == 0 || p.dataAvail() > 0) {
            // either command succeeds or command fails but with data payload
            try {switch (rr.mRequest) {
            /*
            cat libs/telephony/ril_commands.h \
            | egrep "^ *{RIL_" \
            | sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: ret = \2(p); break;/'
             */
            case RIL_REQUEST_GET_SIM_STATUS: ret =  responseIccCardStatus(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK2: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: ret =  responseInts(p); break;
            case RIL_REQUEST_GET_CURRENT_CALLS: ret =  responseCallList(p); break;
            case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
            case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: ret =  responseVoid(p); break;
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CONFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseLastCallFailCause(p); break;
            case RIL_REQUEST_SIGNAL_STRENGTH: ret =  responseSignalStrength(p); break;
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseVoiceRegistrationState(p); break;
            case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_OPERATOR: ret =  responseStrings(p); break;
            case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: ret =  responseSMS(p); break;
            case RIL_REQUEST_SETUP_DATA_CALL: ret =  responseSetupDataCall(p); break;
            case RIL_REQUEST_SIM_IO: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: ret =  responseCallForward(p); break;
            case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
            case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
            case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
            case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : ret =  responseOperatorInfos(p); break;
            case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
            case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
            case RIL_REQUEST_SEPARATE_CONNECTION: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
            case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_DATA_CALL_LIST: ret =  responseDataCallList(p); break;
            case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
            case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
            case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
            case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_BAND_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_STK_GET_PROFILE: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SET_PROFILE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: ret =  responseNetworkType(p); break;
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: ret = responseCellList(p); break;
            case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_FLASH: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BURST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: ret =  responseGmsBroadcastConfig(p); break;
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: ret =  responseCdmaBroadcastConfig(p); break;
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SUBSCRIPTION: ret =  responseCdmaSubscription(p); break;
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: ret = responseVoid(p); break;
            case RIL_REQUEST_DEVICE_IDENTITY: ret =  responseStrings(p); break;
            case RIL_REQUEST_GET_SMSC_ADDRESS: ret = responseString(p); break;
            case RIL_REQUEST_SET_SMSC_ADDRESS: ret = responseVoid(p); break;
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: ret = responseVoid(p); break;
            case RIL_REQUEST_DIAL_EMERGENCY: ret = responseVoid(p); break;
            default:
                throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
                //break;
            }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                        + requestToString(rr.mRequest)
                        + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, null, tr);
                    rr.mResult.sendToTarget();
                }
                return rr;
            }
        }

        if (error != 0) {
            // Ugly fix for Samsung messing up SMS_SEND request fail in binary RIL
            if (error == -1 && rr.mRequest == RIL_REQUEST_SEND_SMS)
            {
                try
                {
                    ret = responseSMS(p);
                } catch (Throwable tr) {
                    Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                            + requestToString(rr.mRequest)
                            + " exception, Processing Samsung SMS fix ", tr);
                    rr.onError(error, ret);
                    return rr;
                }
            } else {
                rr.onError(error, ret);
                return rr;
            }
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }

        return rr;
    }

    @Override
    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr;
        if (!mIsSamsungCdma && PhoneNumberUtils.isEmergencyNumber(address)) {
            dialEmergencyCall(address, clirMode, result);
            return;
        }

        rr = RILRequest.obtain(RIL_REQUEST_DIAL, result);
        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mParcel.writeInt(0); // UUS information is absent
        } else {
            rr.mParcel.writeInt(1); // UUS information is present
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    dialEmergencyCall(String address, int clirMode, Message result) {
        RILRequest rr;
        Rlog.v(RILJ_LOG_TAG, "Emergency dial: " + address);

        rr = RILRequest.obtain(RIL_REQUEST_DIAL_EMERGENCY, result);
        rr.mParcel.writeString(address + "/");
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0);
        rr.mParcel.writeInt(0);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    @Override
    protected void
    processUnsolicited (Parcel p) {
        int response;
        Object ret;
        int dataPosition = p.dataPosition();

        response = p.readInt();

        switch(response) {
        /*
				cat libs/telephony/ril_unsol_commands.h \
				| egrep "^ *{RIL_" \
				| sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: \2(rr, p); break;/'
         */

        case RIL_UNSOL_NITZ_TIME_RECEIVED: ret =  responseString(p); break;
        case RIL_UNSOL_SIGNAL_STRENGTH: ret = responseSignalStrength(p); break;
        case RIL_UNSOL_CDMA_INFO_REC: ret = responseCdmaInformationRecord(p); break;
        case RIL_UNSOL_HSDPA_STATE_CHANGED: ret = responseInts(p); break;
        case RIL_UNSOL_STK_PROACTIVE_COMMAND: ret = responseString(p); break;

        //fixing anoying Exceptions caused by the new Samsung states
        //FIXME figure out what the states mean an what data is in the parcel

        case RIL_UNSOL_O2_HOME_ZONE_INFO: ret = responseVoid(p); break;
        case RIL_UNSOL_DEVICE_READY_NOTI: ret = responseVoid(p); break;
        case RIL_UNSOL_GPS_NOTI: ret = responseVoid(p); break; // Ignored in TW RIL.
        case RIL_UNSOL_SAMSUNG_UNKNOWN_MAGIC_REQUEST: ret = responseVoid(p); break;
        case RIL_UNSOL_SAMSUNG_UNKNOWN_MAGIC_REQUEST_2: ret = responseVoid(p); break;
        case RIL_UNSOL_AM: ret = responseString(p); break;
        
        case 1038: // RIL_UNSOL_DATA_NETWORK_STATE_CHANGED //
            ret = responseVoid(p);
            break;

        default:
            // Rewind the Parcel
            p.setDataPosition(dataPosition);

            // Forward responses that we are not overriding to the super class
            super.processUnsolicited(p);
            return;
        }

        switch(response) {
        case RIL_UNSOL_HSDPA_STATE_CHANGED:
            if (RILJ_LOGD) unsljLog(response);

            boolean newHsdpa = ((int[])ret)[0] == 1;
            String curState = SystemProperties.get(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE);
            boolean curHsdpa = false;

            if (curState.startsWith("HSDPA")) {
                curHsdpa = true;
            } else if (!curState.startsWith("UMTS")) {
                // Don't send poll request if not on 3g
                break;
            }

            if (curHsdpa != newHsdpa) {
                mVoiceNetworkStateRegistrants
                    .notifyRegistrants(new AsyncResult(null, null, null));
            }
            break;

        case RIL_UNSOL_NITZ_TIME_RECEIVED:
            if (RILJ_LOGD) unsljLogRet(response, ret);

            // has bonus long containing milliseconds since boot that the NITZ
            // time was received
            long nitzReceiveTime = p.readLong();

            Object[] result = new Object[2];

            String nitz = (String)ret;
            if (RILJ_LOGD) riljLog(" RIL_UNSOL_NITZ_TIME_RECEIVED length = "
                    + nitz.split("[/:,+-]").length);

            // remove the tailing information that samsung added to the string
            if(nitz.split("[/:,+-]").length >= 9)
                nitz = nitz.substring(0,(nitz.lastIndexOf(",")));

            if (RILJ_LOGD) riljLog(" RIL_UNSOL_NITZ_TIME_RECEIVED striped nitz = "
                    + nitz);

            result[0] = nitz;
            result[1] = Long.valueOf(nitzReceiveTime);

            if (mNITZTimeRegistrant != null) {

                mNITZTimeRegistrant
                .notifyRegistrant(new AsyncResult (null, result, null));
            } else {
                // in case NITZ time registrant isnt registered yet
                mLastNITZTimeInfo = nitz;
            }
            break;

        case RIL_UNSOL_SIGNAL_STRENGTH:
            // Note this is set to "verbose" because it happens
            // frequently
            if (RILJ_LOGV) unsljLogvRet(response, ret);

            if (mSignalStrengthRegistrant != null) {
                mSignalStrengthRegistrant.notifyRegistrant(
                                    new AsyncResult (null, ret, null));
            }
            break;

        case RIL_UNSOL_STK_PROACTIVE_COMMAND:
            if (RILJ_LOGD) unsljLogRet(response, ret);

            if (mCatProCmdRegistrant != null) {
                mCatProCmdRegistrant.notifyRegistrant(
                                    new AsyncResult (null, ret, null));
            } else {
                // The RIL will send a CAT proactive command before the
                // registrant is registered. Buffer it to make sure it
                // does not get ignored (and breaks CatService).
                mCatProCmdBuffer = ret;
            }
            break;

        case RIL_UNSOL_CDMA_INFO_REC:
            ArrayList<CdmaInformationRecords> listInfoRecs;

            try {
                listInfoRecs = (ArrayList<CdmaInformationRecords>)ret;
            } catch (ClassCastException e) {
                Rlog.e(RILJ_LOG_TAG, "Unexpected exception casting to listInfoRecs", e);
                break;
            }

            for (CdmaInformationRecords rec : listInfoRecs) {
                if (RILJ_LOGD) unsljLogRet(response, rec);
                notifyRegistrantsCdmaInfoRec(rec);
            }
            break;

        case RIL_UNSOL_AM:
            String amString = (String) ret;
            Rlog.d(RILJ_LOG_TAG, "Executing AM: " + amString);

            try {
                Runtime.getRuntime().exec("am " + amString);
            } catch (IOException e) {
                e.printStackTrace();
                Rlog.e(RILJ_LOG_TAG, "am " + amString + " could not be executed.");
            }
            break;
        }
    }

    @Override
    protected Object
    responseCallList(Parcel p) {
        int num;
        boolean isVideo;
        ArrayList<DriverCall> response;
        DriverCall dc;
        int dataAvail = p.dataAvail();
        int pos = p.dataPosition();
        int size = p.dataSize();

        Rlog.d(RILJ_LOG_TAG, "Parcel size = " + size);
        Rlog.d(RILJ_LOG_TAG, "Parcel pos = " + pos);
        Rlog.d(RILJ_LOG_TAG, "Parcel dataAvail = " + dataAvail);

        num = p.readInt();
        response = new ArrayList<DriverCall>(num);

        for (int i = 0 ; i < num ; i++) {
            if (mIsSamsungCdma)
                dc = new SamsungDriverCall();
            else
                dc = new DriverCall();

            dc.state                = DriverCall.stateFromCLCC(p.readInt());
            dc.index                = p.readInt();
            dc.TOA                  = p.readInt();
            dc.isMpty               = (0 != p.readInt());
            dc.isMT                 = (0 != p.readInt());
            dc.als                  = p.readInt();
            dc.isVoice              = (0 != p.readInt());
            isVideo                 = (0 != p.readInt());
            dc.isVoicePrivacy       = (0 != p.readInt());
            dc.number               = p.readString();
            int np                  = p.readInt();
            dc.numberPresentation   = DriverCall.presentationFromCLIP(np);
            dc.name                 = p.readString();
            dc.namePresentation     = p.readInt();
            int uusInfoPresent      = p.readInt();

            Rlog.d(RILJ_LOG_TAG, "state = " + dc.state);
            Rlog.d(RILJ_LOG_TAG, "index = " + dc.index);
            Rlog.d(RILJ_LOG_TAG, "state = " + dc.TOA);
            Rlog.d(RILJ_LOG_TAG, "isMpty = " + dc.isMpty);
            Rlog.d(RILJ_LOG_TAG, "isMT = " + dc.isMT);
            Rlog.d(RILJ_LOG_TAG, "als = " + dc.als);
            Rlog.d(RILJ_LOG_TAG, "isVoice = " + dc.isVoice);
            Rlog.d(RILJ_LOG_TAG, "isVideo = " + isVideo);
            Rlog.d(RILJ_LOG_TAG, "number = " + dc.number);
            Rlog.d(RILJ_LOG_TAG, "numberPresentation = " + np);
            Rlog.d(RILJ_LOG_TAG, "name = " + dc.name);
            Rlog.d(RILJ_LOG_TAG, "namePresentation = " + dc.namePresentation);
            Rlog.d(RILJ_LOG_TAG, "uusInfoPresent = " + uusInfoPresent);

            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                dc.uusInfo.setDcs(p.readInt());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                Rlog
                .v(RILJ_LOG_TAG, String.format("Incoming UUS : type=%d, dcs=%d, length=%d",
                        dc.uusInfo.getType(), dc.uusInfo.getDcs(),
                        dc.uusInfo.getUserData().length));
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : data (string)="
                        + new String(dc.uusInfo.getUserData()));
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : data (hex): "
                        + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                Rlog.v(RILJ_LOG_TAG, "Incoming UUS : NOT present!");
            }

            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

            response.add(dc);

            if (dc.isVoicePrivacy) {
                mVoicePrivacyOnRegistrants.notifyRegistrants();
                Rlog.d(RILJ_LOG_TAG, "InCall VoicePrivacy is enabled");
            } else {
                mVoicePrivacyOffRegistrants.notifyRegistrants();
                Rlog.d(RILJ_LOG_TAG, "InCall VoicePrivacy is disabled");
            }
        }

        Collections.sort(response);

        return response;
    }

    protected Object
    responseLastCallFailCause(Parcel p) {
        int response[] = (int[])responseInts(p);

        if (mIsSamsungCdma && response.length > 0 &&
            response[0] == com.android.internal.telephony.cdma.CallFailCause.ERROR_UNSPECIFIED) {

            // Far-end hangup returns ERROR_UNSPECIFIED, which shows "Call Lost" dialog.
            Rlog.d(RILJ_LOG_TAG, "Overriding ERROR_UNSPECIFIED fail cause with NORMAL_CLEARING.");
            response[0] = com.android.internal.telephony.cdma.CallFailCause.NORMAL_CLEARING;
        }

        return response;
    }

    @Override
    protected Object
    responseSignalStrength(Parcel p) {
        // When SIM is PIN-unlocked, the RIL responds with APPSTATE_UNKNOWN and
        // does not follow up with RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED. We
        // notify the system here.
        String state = SystemProperties.get(TelephonyProperties.PROPERTY_SIM_STATE);
        if (!"READY".equals(state) && mIccStatusChangedRegistrants != null && !mIsSamsungCdma) {
            mIccStatusChangedRegistrants.notifyRegistrants();
        }

        int[] response = new int[7];
        for (int i = 0 ; i < 7 ; i++) {
            response[i] = p.readInt();
        }

        if (mIsSamsungCdma){
            if(response[3] < 0){
               response[3] = -response[3];
            }
            // Framework takes care of the rest for us.
        }
        else {
            /* Matching Samsung signal strength to asu.
               Method taken from Samsungs cdma/gsmSignalStateTracker */
            if(mSignalbarCount)
            {
                // Samsung sends the count of bars that should be displayed instead of
                // a real signal strength
                response[0] = ((response[0] & 0xFF00) >> 8) * 3; // gsmDbm
            } else {
                response[0] = response[0] & 0xFF; // gsmDbm
            }
            response[1] = -1; // gsmEcio
            response[2] = (response[2] < 0)?-120:-response[2]; // cdmaDbm
            response[3] = (response[3] < 0)?-160:-response[3]; // cdmaEcio
            response[4] = (response[4] < 0)?-120:-response[4]; // evdoRssi
            response[5] = (response[5] < 0)?-1:-response[5]; // evdoEcio
            if(response[6] < 0 || response[6] > 8)
                response[6] = -1;
        }

        SignalStrength signalStrength = new SignalStrength(
            response[0], response[1], response[2], response[3], response[4],
            response[5], response[6], !mIsSamsungCdma);
        return signalStrength;
    }

    protected Object
    responseVoiceRegistrationState(Parcel p) {
        String response[] = (String[])responseStrings(p);

        if (mIsSamsungCdma && response.length > 6) {
            // These values are provided in hex, convert to dec.
            response[4] = Integer.toString(Integer.parseInt(response[4], 16)); // baseStationId
            response[5] = Integer.toString(Integer.parseInt(response[5], 16)); // baseStationLatitude
            response[6] = Integer.toString(Integer.parseInt(response[6], 16)); // baseStationLongitude
        }

        return response;
    }

    protected Object
    responseNetworkType(Parcel p) {
        int response[] = (int[]) responseInts(p);

        // When the modem responds Phone.NT_MODE_GLOBAL, it means Phone.NT_MODE_WCDMA_PREF
        if (!mIsSamsungCdma && response[0] == Phone.NT_MODE_GLOBAL) {
            Rlog.d(RILJ_LOG_TAG, "Overriding network type response from global to WCDMA preferred");
            response[0] = Phone.NT_MODE_WCDMA_PREF;
        }

        return response;
    }

    @Override
    protected Object
    responseSetupDataCall(Parcel p) {
        DataCallResponse dataCall = new DataCallResponse();
        String strings[] = (String []) responseStrings(p);

        if (strings.length >= 2) {
            dataCall.cid = Integer.parseInt(strings[0]);

            if (mIsSamsungCdma) {
                // We're responsible for starting/stopping the pppd_cdma service.
                if (!startPppdCdmaService(strings[1])) {
                    // pppd_cdma service didn't respond timely.
                    dataCall.status = DcFailCause.ERROR_UNSPECIFIED.getErrorCode();
                    return dataCall;
                }

                // pppd_cdma service responded, pull network parameters set by ip-up script.
                dataCall.ifname = SystemProperties.get("net.cdma.ppp.interface");
                String   ifprop = "net." + dataCall.ifname;

                dataCall.addresses = new String[] {SystemProperties.get(ifprop + ".local-ip")};
                dataCall.gateways  = new String[] {SystemProperties.get(ifprop + ".remote-ip")};
                dataCall.dnses     = new String[] {SystemProperties.get(ifprop + ".dns1"),
                                                   SystemProperties.get(ifprop + ".dns2")};
            } else {
                dataCall.ifname = strings[1];

                if (strings.length >= 3) {
                    dataCall.addresses = strings[2].split(" ");
                }
            }
        } else {
            if (mIsSamsungCdma) {
                // On rare occasion the pppd_cdma service is left active from a stale
                // session, causing the data call setup to fail.  Make sure that pppd_cdma
                // is stopped now, so that the next setup attempt may succeed.
                Rlog.d(RILJ_LOG_TAG, "Set ril.cdma.data_state=0 to make sure pppd_cdma is stopped.");
                SystemProperties.set("ril.cdma.data_state", "0");
            }

            dataCall.status = DcFailCause.ERROR_UNSPECIFIED.getErrorCode(); // Who knows?
        }

        return dataCall;
    }

    private boolean startPppdCdmaService(String ttyname) {
        SystemProperties.set("net.cdma.datalinkinterface", ttyname);

        // Connecting: Set ril.cdma.data_state=1 to (re)start pppd_cdma service,
        // which responds by setting ril.cdma.data_state=2 once connection is up.
        SystemProperties.set("ril.cdma.data_state", "1");
        Rlog.d(RILJ_LOG_TAG, "Set ril.cdma.data_state=1, waiting for ril.cdma.data_state=2.");

        // Typically takes < 200 ms on my Epic, so sleep in 100 ms intervals.
        for (int i = 0; i < 10; i++) {
            try {Thread.sleep(100);} catch (InterruptedException e) {}

            if (SystemProperties.getInt("ril.cdma.data_state", 1) == 2) {
                Rlog.d(RILJ_LOG_TAG, "Got ril.cdma.data_state=2, connected.");
                return true;
            }
        }

        // Taking > 1 s here, try up to 10 s, which is hopefully long enough.
        for (int i = 1; i < 10; i++) {
            try {Thread.sleep(1000);} catch (InterruptedException e) {}

            if (SystemProperties.getInt("ril.cdma.data_state", 1) == 2) {
                Rlog.d(RILJ_LOG_TAG, "Got ril.cdma.data_state=2, connected.");
                return true;
            }
        }

        // Disconnect: Set ril.cdma.data_state=0 to stop pppd_cdma service.
        Rlog.d(RILJ_LOG_TAG, "Didn't get ril.cdma.data_state=2 timely, aborting.");
        SystemProperties.set("ril.cdma.data_state", "0");

        return false;
    }

    @Override
    public void
    deactivateDataCall(int cid, int reason, Message result) {
        if (mIsSamsungCdma) {
            // Disconnect: Set ril.cdma.data_state=0 to stop pppd_cdma service.
            Rlog.d(RILJ_LOG_TAG, "Set ril.cdma.data_state=0.");
            SystemProperties.set("ril.cdma.data_state", "0");
        }

        super.deactivateDataCall(cid, reason, result);
    }

    protected Object
    responseCdmaSubscription(Parcel p) {
        String response[] = (String[])responseStrings(p);

        if (/* mIsSamsungCdma && */ response.length == 4) {
            // PRL version is missing in subscription parcel, add it from properties.
            String prlVersion = SystemProperties.get("ril.prl_ver_1").split(":")[1];
            response          = new String[] {response[0], response[1], response[2],
                                              response[3], prlVersion};
        }

        return response;
    }

    // Workaround for Samsung CDMA "ring of death" bug:
    //
    // Symptom: As soon as the phone receives notice of an incoming call, an
    //   audible "old fashioned ring" is emitted through the earpiece and
    //   persists through the duration of the call, or until reboot if the call
    //   isn't answered.
    //
    // Background: The CDMA telephony stack implements a number of "signal info
    //   tones" that are locally generated by ToneGenerator and mixed into the
    //   voice call path in response to radio RIL_UNSOL_CDMA_INFO_REC requests.
    //   One of these tones, IS95_CONST_IR_SIG_IS54B_L, is requested by the
    //   radio just prior to notice of an incoming call when the voice call
    //   path is muted.  CallNotifier is responsible for stopping all signal
    //   tones (by "playing" the TONE_CDMA_SIGNAL_OFF tone) upon receipt of a
    //   "new ringing connection", prior to unmuting the voice call path.
    //
    // Problem: CallNotifier's incoming call path is designed to minimize
    //   latency to notify users of incoming calls ASAP.  Thus,
    //   SignalInfoTonePlayer requests are handled asynchronously by spawning a
    //   one-shot thread for each.  Unfortunately the ToneGenerator API does
    //   not provide a mechanism to specify an ordering on requests, and thus,
    //   unexpected thread interleaving may result in ToneGenerator processing
    //   them in the opposite order that CallNotifier intended.  In this case,
    //   playing the "signal off" tone first, followed by playing the "old
    //   fashioned ring" indefinitely.
    //
    // Solution: An API change to ToneGenerator is required to enable
    //   SignalInfoTonePlayer to impose an ordering on requests (i.e., drop any
    //   request that's older than the most recent observed).  Such a change,
    //   or another appropriate fix should be implemented in AOSP first.
    //
    // Workaround: Intercept RIL_UNSOL_CDMA_INFO_REC requests from the radio,
    //   check for a signal info record matching IS95_CONST_IR_SIG_IS54B_L, and
    //   drop it so it's never seen by CallNotifier.  If other signal tones are
    //   observed to cause this problem, they should be dropped here as well.
    @Override
    protected void
    notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
        final int response = RIL_UNSOL_CDMA_INFO_REC;

        if (/* mIsSamsungCdma && */ infoRec.record instanceof CdmaSignalInfoRec) {
            CdmaSignalInfoRec sir = (CdmaSignalInfoRec)infoRec.record;
            if (sir != null && sir.isPresent &&
                sir.signalType == SignalToneUtil.IS95_CONST_IR_SIGNAL_IS54B &&
                sir.alertPitch == SignalToneUtil.IS95_CONST_IR_ALERT_MED    &&
                sir.signal     == SignalToneUtil.IS95_CONST_IR_SIG_IS54B_L) {

                Rlog.d(RILJ_LOG_TAG, "Dropping \"" + responseToString(response) + " " +
                      retToString(response, sir) + "\" to prevent \"ring of death\" bug.");
                return;
            }
        }

        super.notifyRegistrantsCdmaInfoRec(infoRec);
    }

    protected class SamsungDriverCall extends DriverCall {
        @Override
        public String
        toString() {
            // Samsung CDMA devices' call parcel is formatted differently
            // fake unused data for video calls, and fix formatting
            // so that voice calls' information can be correctly parsed
            return "id=" + index + ","
            + state + ","
            + "toa=" + TOA + ","
            + (isMpty ? "conf" : "norm") + ","
            + (isMT ? "mt" : "mo") + ","
            + "als=" + als + ","
            + (isVoice ? "voc" : "nonvoc") + ","
            + "nonvid" + ","
            + number + ","
            + "cli=" + numberPresentation + ","
            + "name=" + name + ","
            + namePresentation;
        }
    }

    @Override
    public void setOnCatProactiveCmd(Handler h, int what, Object obj) {
        mCatProCmdRegistrant = new Registrant (h, what, obj);
        if (mCatProCmdBuffer != null) {
            mCatProCmdRegistrant.notifyRegistrant(
                                new AsyncResult (null, mCatProCmdBuffer, null));
            mCatProCmdBuffer = null;
        }
    }

    @Override
    public void getNeighboringCids(Message response) {
        /* RIL_REQUEST_GET_NEIGHBORING_CELL_IDS currently returns REQUEST_NOT_SUPPORTED */

        AsyncResult.forMessage(response).exception =
        new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
        response.sendToTarget();
        response = null;
    }
}