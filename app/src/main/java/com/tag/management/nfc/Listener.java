package com.tag.management.nfc;

public interface Listener {

    void onDialogDisplayed();

    void onDialogDismissed();

    void onDeleteStaff(String staffUId);
}
