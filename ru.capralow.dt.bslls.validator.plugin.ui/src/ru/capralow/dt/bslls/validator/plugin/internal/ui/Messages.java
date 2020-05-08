package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import org.eclipse.osgi.util.NLS;

final class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = " ru.capralow.dt.bslls.validator.plugin.internal.ui.messages"; //$NON-NLS-1$

    public static String BslValidator_Bad_Location_Exception;
    public static String BslValidator_Failed_to_create_injector_for_0;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
