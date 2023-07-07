package io.debezium.util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.debezium.relational.RelationalDatabaseConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.Properties;

public class SSHUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(SSHUtil.class);

  /**
   * Create a SSH connection with the Host using the key from Bucket
   *
   * @param props
   * @return props
   */
  public static Properties connectViaSSH(Properties props) {
    if (props.getProperty(RelationalDatabaseConnectorConfig.SSH_HOSTNAME.name()) != null) {
      LOGGER.info("Starting configureSsh");

      LOGGER.info("Properties got: {}", props);
      try {
        JSch jSch = new JSch();

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        if (Objects.isNull(props.getProperty(RelationalDatabaseConnectorConfig.SSH_PASSWORD.name()))) {
          if (props.containsKey(RelationalDatabaseConnectorConfig.SSH_PRIVATE_KEY.name())) {
            jSch.addIdentity(
                props.getProperty(RelationalDatabaseConnectorConfig.HOSTNAME.name()),
                props.getProperty(RelationalDatabaseConnectorConfig.SSH_PRIVATE_KEY.name()).getBytes(),
                props.getProperty(RelationalDatabaseConnectorConfig.SSH_PUBLIC_KEY.name(), "").getBytes(),
                props.getProperty(RelationalDatabaseConnectorConfig.SSH_PASSPHRASE.name(), "").getBytes());
          }
        }

        String hostIP = props.getProperty(RelationalDatabaseConnectorConfig.HOSTNAME.name());
        String hostPort = props.getProperty(RelationalDatabaseConnectorConfig.PORT.name());

        int lport = getLocalTunnelport();

        Session session =
            jSch.getSession(
                props.getProperty(RelationalDatabaseConnectorConfig.SSH_USER.name()),
                props.getProperty(RelationalDatabaseConnectorConfig.SSH_HOSTNAME.name()),
                Integer.parseInt(props.getProperty(RelationalDatabaseConnectorConfig.SSH_PORT.name())));

        if (Objects.nonNull(props.getProperty(RelationalDatabaseConnectorConfig.SSH_PASSWORD.name()))) {
          session.setPassword(props.getProperty(RelationalDatabaseConnectorConfig.SSH_PASSWORD.name()));
        }

        session.setPortForwardingL(lport, hostIP,
            Integer.parseInt(hostPort));
        session.setConfig(config);
        session.connect();

        props.setProperty(RelationalDatabaseConnectorConfig.HOSTNAME.name(), "127.0.0.1");
        props.setProperty(RelationalDatabaseConnectorConfig.PORT.name(), String.valueOf(lport));
      } catch (Exception e) {
        LOGGER.error("Error in connectViaSSH", e);
        // TODO
      }

      LOGGER.info("Returning properties: {}", props);
    }
    return props;
  }

  /**
   * Get the available Port in Local machine
   *
   * @return
   */
  private static int getLocalTunnelport() throws IOException {
    try (ServerSocket socket = new ServerSocket()) {
      InetSocketAddress randomSocketAddressFirst = new InetSocketAddress(0);
      socket.bind(randomSocketAddressFirst);
      return socket.getLocalPort();
    } catch (IOException e) {
      LOGGER.error("Error in getLocalTunnelport", e);
      throw e;
    }
  }
}
