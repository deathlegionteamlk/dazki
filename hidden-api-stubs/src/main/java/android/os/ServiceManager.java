package android.os;

/**
 * Compile-time stub for the hidden ServiceManager class. At runtime on
 * a real device, the platform implementation is used. This stub exists
 * only so the manager and server modules compile against the SDK.
 */
public class ServiceManager {

    public static IBinder getService(String name) {
        throw new UnsupportedOperationException("stub");
    }

    public static void addService(String name, IBinder service) {
        throw new UnsupportedOperationException("stub");
    }

    public static String[] listServices() {
        throw new UnsupportedOperationException("stub");
    }
}
