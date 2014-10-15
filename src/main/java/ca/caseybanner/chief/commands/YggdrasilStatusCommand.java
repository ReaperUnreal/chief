package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import com.bigvikinggames.bam.BamLink;
import com.bigvikinggames.bam.netty.client.NettyBamClient;
import com.bigvikinggames.yggdrasil.protocol.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Chris on 10/14/14.
 */
public class YggdrasilStatusCommand extends Command {

    private class YggdrasilStatusProtocolHandler implements YggdrasilStatusClientProtocolHandler {

        private CompletableFuture<Optional<String>> response;

        public YggdrasilStatusProtocolHandler() {
            response = new CompletableFuture<>();
        }

        @Override
        public void onServiceRegisteredMessage(BamLink bamLink, ServiceRegisteredMessage serviceRegisteredMessage) {

        }

        @Override
        public void onServiceDisconnectedMessage(BamLink bamLink, ServiceDisconnectedMessage serviceDisconnectedMessage) {

        }

        @Override
        public void onServiceStatusMessage(BamLink bamLink, ServiceStatusMessage serviceStatusMessage) {

        }

        @Override
        public void onUnknownServiceNameMessage(BamLink bamLink, UnknownServiceNameMessage unknownServiceNameMessage) {

        }

        @Override
        public void onServiceListMessage(BamLink bamLink, ServiceListMessage serviceListMessage) {
            StringBuilder result = new StringBuilder();

            serviceListMessage.getServices().stream().forEach(service -> {
                String serviceName = service.getServiceName();
                result.append(serviceName + ":\n");
                result.append("\tProviders:\n");

                service.getProviders().forEach(provider -> {
                    String host = provider.getHost();
                    int port = provider.getPort();
                    boolean isOnline = provider.getIsOnline();

                    result.append("\t\t" + host + ":" + port + " (" + (isOnline ? "ONLINE" : "OFFLINE") + "\n");
                    provider.getProtocols().forEach(protocol -> {
                        result.append("\t\t\t" + protocol.getName() + "(" + protocol.getProtocolIdHash() + ")\n");
                    });
                });

                result.append("\tClients:\n");
                service.getClients().forEach(client -> {
                    String connectionName = client.getConnectionName();
                    result.append("\t\t" + connectionName + "\n");

                    client.getProtocols().forEach(protocol -> {
                        result.append("\t\t\t" + protocol.getName() + "(" + protocol.getProtocolIdHash() + ")\n");
                    });
                });

                result.append("\n---\n\n");
            });

            response.complete(Optional.of(result.toString()));
        }

        @Override
        public void linkAdded(BamLink bamLink) {

        }

        @Override
        public void linkRemoved(BamLink bamLink) {

        }

        public CompletableFuture<Optional<String>> getStatus() {
            return response;
        }
    }

    /**
     * Logger for anything of interest
     */
    private static final Logger logger = LogManager.getLogger(YggdrasilStatusCommand.class);

    /**
     * Matches `ygg-status list` or `ygg-status status <serverName>`
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^ygg-status\\s+(?<command>list|status)\\s*(?<serverName>.+)?$"
    );

    /**
     * List of servers we know about
     */
    private Map<String, String> servers;

    /**
     * Client to talk to yggdrasil servers on
     */
    private Map<String, NettyBamClient> bamClients;

    /**
     * Constructor
     *
     * @param bot the Bot this command belongs to
     */
    protected YggdrasilStatusCommand(Bot bot) {
        super(bot);

        servers = new HashMap<>();
        bamClients = new HashMap<>();
    }

    /**
     * Set the server list.  This is expected to be a series of csv entries of the form
     * <serverName>=<serverConnectionString>
     *
     * @param serverList
     */
    public void setServerList(String serverList) {
        String[] pairs = serverList.split("\\s*,\\s*");
        Arrays.stream(pairs).forEach(pair -> {
            String[] split = pair.split("\\s*=\\s*");

            if (split.length != 2) {
                logger.error("Invalid configuration entry: " + pair);
                return;
            }

            servers.put(split[0], split[1]);
        });

        if (servers.size() == 0) {
            logger.warn("No servers were configured.  This is going to be boring.");
        }
    }

    /**
     * Returns a helpful string about how to use this command
     *
     * @return
     */
    @Override
    public String getUsage() {
        return "ygg-status list - list known yggdrasil servers\n" +
                "ygg-status status <serverName> - list a server's status\n";
    }

    /**
     * Get the pattern for text this command should handle
     *
     * @return
     */
    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    /**
     * Process a message
     *
     * @param from the nickname the chat was from
     * @param message the message itself
     * @param matcher the matcher created by pattern returned by getPattern(),
     *	               already run on the message
     * @param fromRoom whether or not this message come from a room
     * @return
     */
    @Override
    public CompletableFuture<Optional<String>> processAsyncMessage(String from, String message, Matcher matcher, boolean fromRoom) {
        String command = matcher.group("command");

        switch (command) {
            case "list":
                return toFuture(getList());
            case "status":
                String serverName = matcher.group("serverName");
                if (null == serverName) {
                    return toFuture("Please provide a server name");
                }

                if (! servers.containsKey(serverName)) {
                    return toFuture("Cannot find a server by the name " + serverName);
                }

                return getStatus(serverName);
            default:
                return toFuture("I don't understand that command :(");
        }
    }

    /**
     * Get the list of servers that we know
     *
     * @return
     */
    private Optional<String> getList() {
        if (servers.size() == 0) {
            return Optional.of("I don't know about any servers :(");
        }

        StringBuilder response = new StringBuilder("I know about these servers:\n");

        servers.entrySet().stream().forEach(entry -> {
            String serverName = entry.getKey();
            String connectionString = entry.getValue();

            response.append("\t" + serverName + ": " + connectionString + "\n");
        });

        return Optional.of(response.toString());
    }

    /**
     * Get the status of a server
     *
     * @return
     */
    private CompletableFuture<Optional<String>> getStatus(String serverName) {
        String connectionString = servers.get(serverName);
        String[] hostnamePortSplit = connectionString.split(":");
        int port = Integer.parseInt(hostnamePortSplit[1]);

        YggdrasilStatusProtocolHandler handler = new YggdrasilStatusProtocolHandler();
        YggdrasilStatusClientMessageDispatcher dispatcher = new YggdrasilStatusClientMessageDispatcher(handler);
        YggdrasilStatusClientProtocol protocol = new YggdrasilStatusClientProtocol(dispatcher);

        NettyBamClient bamClient = new NettyBamClient(hostnamePortSplit[0], port);
        bamClient.addProtocol(protocol);

        CompletableFuture<Optional<String>> result = null;
        try {
            bamClient.connect();
        } catch (InterruptedException e) {
            return toFuture("Error getting status for " + serverName);
        }

        return handler.getStatus().thenApply(res -> {
            bamClient.disconnect();
            return res;
        });
    }
}
