
/**
 * This class is used to initiate or skip registration using OOBE Flow
 * (running in another process).
 */

package com.mcafee.verizonoobe.aidl;
import android.os.Bundle;

interface IVZWMMSOOBERegistration {

    /**
     * Call this method to initiate registration
     */
    void initiateRegistration(in Bundle parameters);

    /**
     * Call this method to skip registration
     */
    void skipRegistration(in Bundle parameters);

}
