package cn.claresun.cpush.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.CharArrayWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by claresun on 16-8-17.
 */
public class APNSPayloadBuilder {
    private String alertBody = null;
    private String localizedAlertKey = null;
    private String[] localizedAlertArguments = null;
    private String alertTitle = null;
    private String localizedAlertTitleKey = null;
    private String[] localizedAlertTitleArguments = null;
    private String launchImageFileName = null;
    private boolean showActionButton = true;
    private String localizedActionButtonKey = null;
    private Integer badgeNumber = null;
    private String soundFileName = null;
    private String categoryName = null;
    private boolean contentAvailable = false;

    private final CharArrayWriter buffer = new CharArrayWriter(DEFAULT_PAYLOAD_SIZE / 4);

    private static final String APS_KEY = "aps";
    private static final String ALERT_KEY = "alert";
    private static final String BADGE_KEY = "badge";
    private static final String SOUND_KEY = "sound";
    private static final String CATEGORY_KEY = "category";
    private static final String CONTENT_AVAILABLE_KEY = "content-available";

    private static final String ALERT_TITLE_KEY = "title";
    private static final String ALERT_BODY_KEY = "body";
    private static final String ALERT_TITLE_LOC_KEY = "title-loc-key";
    private static final String ALERT_TITLE_ARGS_KEY = "title-loc-args";
    private static final String ACTION_LOC_KEY = "action-loc-key";
    private static final String ALERT_LOC_KEY = "loc-key";
    private static final String ALERT_ARGS_KEY = "loc-args";
    private static final String LAUNCH_IMAGE_KEY = "launch-image";

    private final HashMap<String, Object> customProperties = new HashMap<String, Object>();

    private static final int DEFAULT_PAYLOAD_SIZE = 4096;

    private static final String ABBREVIATION_SUBSTRING = "…";

    private static final Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    public static final String DEFAULT_SOUND_FILENAME = "default";

    public APNSPayloadBuilder() {
    }

    public APNSPayloadBuilder setAlertBody(final String alertBody) {
        if (alertBody != null && this.localizedAlertKey != null) {
            throw new IllegalStateException(
                    "Cannot set a literal alert body when a localized alert key has already been set.");
        }

        this.alertBody = alertBody;

        return this;
    }

    public APNSPayloadBuilder setLocalizedAlertMessage(final String localizedAlertKey, final String[] alertArguments) {
        if (localizedAlertKey != null && this.alertBody != null) {
            throw new IllegalStateException(
                    "Cannot set a localized alert key when a literal alert body has already been set.");
        }

        if (localizedAlertKey == null && alertArguments != null) {
            throw new IllegalArgumentException(
                    "Cannot set localized alert arguments without a localized alert message key.");
        }

        this.localizedAlertKey = localizedAlertKey;
        this.localizedAlertArguments = alertArguments;

        return this;
    }

    public APNSPayloadBuilder setAlertTitle(final String alertTitle) {
        if (alertTitle != null && this.localizedAlertTitleKey != null) {
            throw new IllegalStateException(
                    "Cannot set a literal alert title when a localized alert title key has already been set.");
        }

        this.alertTitle = alertTitle;

        return this;
    }

    public APNSPayloadBuilder setLocalizedAlertTitle(final String localizedAlertTitleKey,
                                                     final String[] alertTitleArguments) {
        if (localizedAlertTitleKey != null && this.alertTitle != null) {
            throw new IllegalStateException(
                    "Cannot set a localized alert key when a literal alert body has already been set.");
        }

        if (localizedAlertTitleKey == null && alertTitleArguments != null) {
            throw new IllegalArgumentException(
                    "Cannot set localized alert arguments without a localized alert message key.");
        }

        this.localizedAlertTitleKey = localizedAlertTitleKey;
        this.localizedAlertTitleArguments = alertTitleArguments;

        return this;
    }

    public APNSPayloadBuilder setLaunchImageFileName(final String launchImageFilename) {
        this.launchImageFileName = launchImageFilename;
        return this;
    }

    public APNSPayloadBuilder setShowActionButton(final boolean showActionButton) {
        this.showActionButton = showActionButton;
        return this;
    }

    public APNSPayloadBuilder setLocalizedActionButtonKey(final String localizedActionButtonKey) {
        this.localizedActionButtonKey = localizedActionButtonKey;
        return this;
    }

    public APNSPayloadBuilder setBadgeNumber(final Integer badgeNumber) {
        this.badgeNumber = badgeNumber;
        return this;
    }

    public APNSPayloadBuilder setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public APNSPayloadBuilder setSoundFileName(final String soundFileName) {
        this.soundFileName = soundFileName;
        return this;
    }

    public APNSPayloadBuilder setContentAvailable(final boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
        return this;
    }

    public APNSPayloadBuilder addCustomProperty(final String key, final Object value) {
        this.customProperties.put(key, value);
        return this;
    }

    public String buildWithDefaultMaximumLength() {
        return this.buildWithMaximumLength(DEFAULT_PAYLOAD_SIZE);
    }

    public String buildWithMaximumLength(final int maximumPayloadSize) {
        final Map<String, Object> payload = new HashMap<String, Object>();

        {
            final Map<String, Object> aps = new HashMap<String, Object>();

            if (this.badgeNumber != null) {
                aps.put(BADGE_KEY, this.badgeNumber);
            }

            if (this.soundFileName != null) {
                aps.put(SOUND_KEY, this.soundFileName);
            }

            if (this.categoryName != null) {
                aps.put(CATEGORY_KEY, this.categoryName);
            }

            if (this.contentAvailable) {
                aps.put(CONTENT_AVAILABLE_KEY, 1);
            }

            final Object alertObject = this.createAlertObject();

            if (alertObject != null) {
                aps.put(ALERT_KEY, alertObject);
            }

            payload.put(APS_KEY, aps);
        }

        for (final Map.Entry<String, Object> entry : this.customProperties.entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }

        this.buffer.reset();
        gson.toJson(payload, this.buffer);

        final String payloadString = this.buffer.toString();
        final int initialPayloadSize = payloadString.getBytes(StandardCharsets.UTF_8).length;

        final String fittedPayloadString;

        if (initialPayloadSize <= maximumPayloadSize) {
            fittedPayloadString = payloadString;
        } else {
            if (this.alertBody != null) {
                this.replaceMessageBody(payload, "");

                this.buffer.reset();
                gson.toJson(payload, this.buffer);

                final int payloadSizeWithEmptyMessage = this.buffer.toString().getBytes(StandardCharsets.UTF_8).length;

                if (payloadSizeWithEmptyMessage >= maximumPayloadSize) {
                    throw new IllegalArgumentException("Payload exceeds maximum size even with an empty message body.");
                }

                final int maximumEscapedMessageBodySize = maximumPayloadSize - payloadSizeWithEmptyMessage -
                        ABBREVIATION_SUBSTRING.getBytes(StandardCharsets.UTF_8).length;

                final String fittedMessageBody = this.alertBody.substring(0,
                        APNSPayloadBuilder.getLengthOfJsonEscapedUtf8StringFittingSize(this.alertBody, maximumEscapedMessageBodySize));

                this.replaceMessageBody(payload, fittedMessageBody + ABBREVIATION_SUBSTRING);

                this.buffer.reset();
                gson.toJson(payload, this.buffer);

                fittedPayloadString = this.buffer.toString();
            } else {
                throw new IllegalArgumentException(String.format(
                        "Payload size is %d bytes (with a maximum of %d bytes) and cannot be shortened.",
                        initialPayloadSize, maximumPayloadSize));
            }
        }

        return fittedPayloadString;
    }

    private void replaceMessageBody(final Map<String, Object> payload, final String messageBody) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> aps = (Map<String, Object>) payload.get(APS_KEY);
        final Object alert = aps.get(ALERT_KEY);

        if (alert != null) {
            if (alert instanceof String) {
                aps.put(ALERT_KEY, messageBody);
            } else {
                @SuppressWarnings("unchecked")
                final Map<String, Object> alertObject = (Map<String, Object>) alert;

                if (alertObject.get(ALERT_BODY_KEY) != null) {
                    alertObject.put(ALERT_BODY_KEY, messageBody);
                } else {
                    throw new IllegalArgumentException("Payload has no message body.");
                }
            }
        } else {
            throw new IllegalArgumentException("Payload has no message body.");
        }
    }

    private Object createAlertObject() {
        if (this.hasAlertContent()) {
            if (this.shouldRepresentAlertAsString()) {
                return this.alertBody;
            } else {
                final HashMap<String, Object> alert = new HashMap<String, Object>();

                if (this.alertBody != null) {
                    alert.put(ALERT_BODY_KEY, this.alertBody);
                }

                if (this.alertTitle != null) {
                    alert.put(ALERT_TITLE_KEY, this.alertTitle);
                }

                if (this.showActionButton) {
                    if (this.localizedActionButtonKey != null) {
                        alert.put(ACTION_LOC_KEY, this.localizedActionButtonKey);
                    }
                } else {
                    // To hide the action button, the key needs to be present, but the value needs to be null
                    alert.put(ACTION_LOC_KEY, null);
                }

                if (this.localizedAlertKey != null) {
                    alert.put(ALERT_LOC_KEY, this.localizedAlertKey);

                    if (this.localizedAlertArguments != null) {
                        alert.put(ALERT_ARGS_KEY, Arrays.asList(this.localizedAlertArguments));
                    }
                }

                if (this.localizedAlertTitleKey != null) {
                    alert.put(ALERT_TITLE_LOC_KEY, this.localizedAlertTitleKey);

                    if (this.localizedAlertTitleArguments != null) {
                        alert.put(ALERT_TITLE_ARGS_KEY, Arrays.asList(this.localizedAlertTitleArguments));
                    }
                }

                if (this.launchImageFileName != null) {
                    alert.put(LAUNCH_IMAGE_KEY, this.launchImageFileName);
                }

                return alert;
            }
        } else {
            return null;
        }
    }

    private boolean hasAlertContent() {
        return this.alertBody != null || this.alertTitle != null || this.localizedAlertTitleKey != null
                || this.localizedAlertKey != null || this.localizedActionButtonKey != null
                || this.launchImageFileName != null || this.showActionButton == false;
    }

    private boolean shouldRepresentAlertAsString() {
        return this.alertBody != null && this.launchImageFileName == null && this.showActionButton
                && this.localizedActionButtonKey == null && this.alertTitle == null
                && this.localizedAlertTitleKey == null && this.localizedAlertKey == null
                && this.localizedAlertArguments == null && this.localizedAlertTitleArguments == null;
    }

    static int getLengthOfJsonEscapedUtf8StringFittingSize(final String string, final int maximumSize) {
        int i = 0;
        int cumulativeSize = 0;

        for (i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            final int charSize = getSizeOfJsonEscapedUtf8Character(c);

            if (cumulativeSize + charSize > maximumSize) {
                // The next character would put us over the edge; bail out here.
                break;
            }

            cumulativeSize += charSize;

            if (Character.isHighSurrogate(c)) {
                // Skip the next character
                i++;
            }
        }

        return i;
    }

    static int getSizeOfJsonEscapedUtf8Character(char c) {
        final int charSize;

        if (c == '"' || c == '\\' || c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t') {
            // Character is backslash-escaped in JSON
            charSize = 2;
        } else if (c <= 0x001F || c == '\u2028' || c == '\u2029') {
            // Character will be represented as an escaped control character
            charSize = 6;
        } else {
            // The character will be represented as an un-escaped UTF8 character
            if (c <= 0x007F) {
                charSize = 1;
            } else if (c <= 0x07FF) {
                charSize = 2;
            } else if (Character.isHighSurrogate(c)) {
                charSize = 4;
            } else {
                charSize = 3;
            }
        }

        return charSize;
    }
}
