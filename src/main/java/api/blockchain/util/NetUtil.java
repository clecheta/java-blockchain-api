package api.blockchain.util;

import io.quarkus.runtime.configuration.ProfileManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetUtil {

    public static final String profile = ProfileManager.getActiveProfile();

    public static String getHostIp() {
        String hostIp = "";
        try {
            InetAddress ip = InetAddress.getLocalHost();
            hostIp = ip.toString();
            System.out.println("Current IP address: " + hostIp);
        } catch (UnknownHostException e) {
            hostIp = "Unknown Host";
            e.printStackTrace();
        }
        return hostIp;
    }

    public static String getHostName() {
        String hostname = "";
        if (profile.equals("node")) { //necessary for testing replace chain between two nodes
            hostname = "node";
        } else {
            try {
                InetAddress ip = InetAddress.getLocalHost();
                hostname = ip.getHostName();
                //System.out.println("Current hostname: " + hostname);
            } catch (UnknownHostException e) {
                hostname = "Unknown Host";
                e.printStackTrace();
            }
        }
        return hostname;
    }

}
