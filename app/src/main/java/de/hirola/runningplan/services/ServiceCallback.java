package de.hirola.runningplan.services;

import android.content.ServiceConnection;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Callback for A background services.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public interface ServiceCallback {
    String SERVICE_RECEIVER_ACTION = "RunningPlan_Service_Receiver_Action";
    String SERVICE_RECEIVER_INTENT_EXRAS_DURATION = "actualDuration";
    void onServiceConnected(ServiceConnection connection);
    default void onServiceDisconnected(ServiceConnection connection) {}
    default void onServiceErrorOccurred(String errorMessage) {}
}
