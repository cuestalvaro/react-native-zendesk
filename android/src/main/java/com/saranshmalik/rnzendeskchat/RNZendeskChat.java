package com.saranshmalik.rnzendeskchat;

import android.app.Activity;
import android.content.Context;

import android.graphics.Color;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.lang.String;

import javax.annotation.Nullable;
import zendesk.chat.Chat;
import zendesk.chat.ChatConfiguration;
import zendesk.chat.ChatEngine;
import zendesk.chat.ChatProvider;
import zendesk.chat.ChatSessionStatus;
import zendesk.chat.ChatState;
import zendesk.chat.ObservationScope;
import zendesk.chat.Observer;
import zendesk.chat.PreChatFormFieldStatus;
import zendesk.chat.ProfileProvider;
import zendesk.chat.PushNotificationsProvider;
import zendesk.chat.Providers;
import zendesk.chat.VisitorInfo;
import zendesk.core.JwtIdentity;
import zendesk.core.AnonymousIdentity;
import zendesk.core.Identity;
import zendesk.messaging.MessagingActivity;
import zendesk.core.Zendesk;
import zendesk.support.Support;
import zendesk.support.guide.HelpCenterActivity;
import zendesk.support.guide.ViewArticleActivity;
import zendesk.support.requestlist.RequestListActivity;
import zendesk.answerbot.AnswerBot;
import zendesk.answerbot.AnswerBotEngine;
// import zendesk.support.SupportEngine;

public class RNZendeskChat extends ReactContextBaseJavaModule {

    private ReactContext appContext;
    private static final String TAG = "ZendeskChat";

    public RNZendeskChat(ReactApplicationContext reactContext) {
        super(reactContext);
        appContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNZendeskChat";
    }

    @ReactMethod
    public void setVisitorInfo(ReadableMap options) {

        Providers providers = Chat.INSTANCE.providers();
        if (providers == null) {
            Log.d(TAG, "Can't set visitor info, provider is null");
            return;
        }
        ProfileProvider profileProvider = providers.profileProvider();
        if (profileProvider == null) {
            Log.d(TAG, "Profile provider is null");
            return;
        }
        ChatProvider chatProvider = providers.chatProvider();
        if (chatProvider == null) {
            Log.d(TAG, "Chat provider is null");
            return;
        }
        VisitorInfo.Builder builder = VisitorInfo.builder();
        if (options.hasKey("name")) {
            builder = builder.withName(options.getString("name"));
        }
        if (options.hasKey("email")) {
            builder = builder.withEmail(options.getString("email"));
        }
        if (options.hasKey("phone")) {
            builder = builder.withPhoneNumber(options.getString("phone"));
        }
        VisitorInfo visitorInfo = builder.build();
        profileProvider.setVisitorInfo(visitorInfo, null);
        if (options.hasKey("department"))
            chatProvider.setDepartment(options.getString("department"), null);

    }

    @ReactMethod
    public void init(ReadableMap options) {
        String appId = options.getString("appId");
        String clientId = options.getString("clientId");
        String url = options.getString("url");
        String key = options.getString("key");
        Context context = appContext;
        Zendesk.INSTANCE.init(context, url, appId, clientId);
        Support.INSTANCE.init(Zendesk.INSTANCE);
        AnswerBot.INSTANCE.init(Zendesk.INSTANCE, Support.INSTANCE);
        Chat.INSTANCE.init(context, key);
    }

    @ReactMethod
    public void initChat(String key) {
        Context context = appContext;
        Chat.INSTANCE.init(context, key);
    }

    @ReactMethod
    public void setUserIdentity(ReadableMap options) {
        if (options.hasKey("token")) {
            Identity identity = new JwtIdentity(options.getString("token"));
            Zendesk.INSTANCE.setIdentity(identity);
        } else {
            String name = options.getString("name");
            String email = options.getString("email");
            Identity identity = new AnonymousIdentity.Builder()
                .withNameIdentifier(name).withEmailIdentifier(email).build();
            Zendesk.INSTANCE.setIdentity(identity);
        }
    }

    @ReactMethod
    public void showHelpCenter(ReadableMap options) {
        String botName = options.hasKey("botName") ? options.getString("botName") : "Chat Bot";
        Activity activity = getCurrentActivity();
        if (options.hasKey("withChat")) {
            HelpCenterActivity.builder()
                .withEngines(ChatEngine.engine())
                .show(activity);
        } else if (options.hasKey("disableTicketCreation")) {
            HelpCenterActivity.builder()
                .withContactUsButtonVisible(false)
                .withShowConversationsMenuButton(false)
                .show(
                    activity,
                    ViewArticleActivity.builder()
                        .withContactUsButtonVisible(false)
                        .config()
                );
        } else {
            HelpCenterActivity.builder()
                .show(activity);
        }
    }

    @ReactMethod
    public void startChat(ReadableMap options) {
        Providers providers = Chat.INSTANCE.providers();
        setUserIdentity(options);
        setVisitorInfo(options);
        setUserIdentity(options);
        String botName = options.hasKey("botName") ? options.getString("botName") : "Chat Bot";
        ChatConfiguration chatConfiguration = ChatConfiguration.builder()
                .withAgentAvailabilityEnabled(true)
                .withOfflineFormEnabled(true)
                .build();

        Activity activity = getCurrentActivity();
        if (options.hasKey("chatOnly")) {
            MessagingActivity.builder()
                .withEngines(ChatEngine.engine())
                .show(activity, chatConfiguration);
        } else {
            MessagingActivity.builder()
                .withEngines(ChatEngine.engine())
                .withBotLabelString(botName)
                .show(activity);
        }
    }

    @ReactMethod
    public void setNotificationToken(String token) {
        PushNotificationsProvider pushProvider = Chat.INSTANCE.providers().pushNotificationsProvider();
        if (pushProvider != null) {
            pushProvider.registerPushToken(token);
        }
    }
}
