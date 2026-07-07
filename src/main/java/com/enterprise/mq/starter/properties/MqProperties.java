package com.enterprise.mq.starter.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Root configuration properties for IBM MQ integration.
 */
@Validated
@ConfigurationProperties(prefix = "mq")
public class MqProperties {

  private boolean enabled = true;

  @NotEmpty(message = "At least one queue manager must be configured")
  @Valid
  private List<QueueManagerProperties> queueManagers = new ArrayList<>();

  @Valid
  private RoutingProperties routing = new RoutingProperties();

  @Valid
  private RetryProperties retry = new RetryProperties();

  @Valid
  private ConnectionPoolProperties pool = new ConnectionPoolProperties();

  @Valid
  private HealthProperties health = new HealthProperties();

  @Valid
  private FailureProperties failure = new FailureProperties();

  @Valid
  private ObservabilityProperties observability = new ObservabilityProperties();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<QueueManagerProperties> getQueueManagers() {
    return queueManagers;
  }

  public void setQueueManagers(List<QueueManagerProperties> queueManagers) {
    this.queueManagers = queueManagers;
  }

  public RoutingProperties getRouting() {
    return routing;
  }

  public void setRouting(RoutingProperties routing) {
    this.routing = routing;
  }

  public RetryProperties getRetry() {
    return retry;
  }

  public void setRetry(RetryProperties retry) {
    this.retry = retry;
  }

  public ConnectionPoolProperties getPool() {
    return pool;
  }

  public void setPool(ConnectionPoolProperties pool) {
    this.pool = pool;
  }

  public HealthProperties getHealth() {
    return health;
  }

  public void setHealth(HealthProperties health) {
    this.health = health;
  }

  public FailureProperties getFailure() {
    return failure;
  }

  public void setFailure(FailureProperties failure) {
    this.failure = failure;
  }

  public ObservabilityProperties getObservability() {
    return observability;
  }

  public void setObservability(ObservabilityProperties observability) {
    this.observability = observability;
  }

  /**
   * Queue manager connection definition.
   */
  public static class QueueManagerProperties {

    @NotBlank
    private String name;

    @NotBlank
    private String host;

    @Min(1)
    private int port = 1414;

    @NotBlank
    private String queueManager;

    @NotBlank
    private String channel;

    private String username;

    private String password;

    private int priority = 0;

    private boolean enabled = true;

    @Valid
    private List<QueueProperties> queues = new ArrayList<>();

    private Map<String, String> properties = new HashMap<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getQueueManager() {
      return queueManager;
    }

    public void setQueueManager(String queueManager) {
      this.queueManager = queueManager;
    }

    public String getChannel() {
      return channel;
    }

    public void setChannel(String channel) {
      this.channel = channel;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public int getPriority() {
      return priority;
    }

    public void setPriority(int priority) {
      this.priority = priority;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public List<QueueProperties> getQueues() {
      return queues;
    }

    public void setQueues(List<QueueProperties> queues) {
      this.queues = queues;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }

  /**
   * Queue definition within a queue manager.
   */
  public static class QueueProperties {

    @NotBlank
    private String name;

    private String alias;

    private boolean requestReplyEnabled;

    private String replyQueue;

    private long receiveTimeoutMs = 5000L;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAlias() {
      return alias;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

    public boolean isRequestReplyEnabled() {
      return requestReplyEnabled;
    }

    public void setRequestReplyEnabled(boolean requestReplyEnabled) {
      this.requestReplyEnabled = requestReplyEnabled;
    }

    public String getReplyQueue() {
      return replyQueue;
    }

    public void setReplyQueue(String replyQueue) {
      this.replyQueue = replyQueue;
    }

    public long getReceiveTimeoutMs() {
      return receiveTimeoutMs;
    }

    public void setReceiveTimeoutMs(long receiveTimeoutMs) {
      this.receiveTimeoutMs = receiveTimeoutMs;
    }
  }

  /**
   * Connection routing configuration.
   */
  public static class RoutingProperties {

    private RoutingStrategy strategy = RoutingStrategy.ROUND_ROBIN;

    private String customStrategyBean;

    public RoutingStrategy getStrategy() {
      return strategy;
    }

    public void setStrategy(RoutingStrategy strategy) {
      this.strategy = strategy;
    }

    public String getCustomStrategyBean() {
      return customStrategyBean;
    }

    public void setCustomStrategyBean(String customStrategyBean) {
      this.customStrategyBean = customStrategyBean;
    }
  }

  public enum RoutingStrategy {
    ROUND_ROBIN,
    FAILOVER,
    PRIORITY
  }

  /**
   * Retry policy configuration.
   */
  public static class RetryProperties {

    private boolean enabled = true;

    @Min(0)
    private int maxAttempts = 3;

    @Min(0)
    private long initialDelayMs = 500L;

    @Min(0)
    private long maxDelayMs = 10000L;

    private double multiplier = 2.0;

    private boolean exponentialBackoff = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getInitialDelayMs() {
      return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
      this.initialDelayMs = initialDelayMs;
    }

    public long getMaxDelayMs() {
      return maxDelayMs;
    }

    public void setMaxDelayMs(long maxDelayMs) {
      this.maxDelayMs = maxDelayMs;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }

    public boolean isExponentialBackoff() {
      return exponentialBackoff;
    }

    public void setExponentialBackoff(boolean exponentialBackoff) {
      this.exponentialBackoff = exponentialBackoff;
    }
  }

  /**
   * JMS connection pool settings.
   */
  public static class ConnectionPoolProperties {

    private boolean enabled = true;

    @Min(1)
    private int maxConnections = 10;

    @Min(1)
    private int maxSessionsPerConnection = 500;

    private boolean blockIfFull = true;

    @Min(0)
    private long connectionTimeoutMs = 30000L;

    @Min(0)
    private long idleTimeoutMs = 30000L;

    private boolean validateOnBorrow = true;

    private boolean reconnectOnException = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMaxConnections() {
      return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }

    public int getMaxSessionsPerConnection() {
      return maxSessionsPerConnection;
    }

    public void setMaxSessionsPerConnection(int maxSessionsPerConnection) {
      this.maxSessionsPerConnection = maxSessionsPerConnection;
    }

    public boolean isBlockIfFull() {
      return blockIfFull;
    }

    public void setBlockIfFull(boolean blockIfFull) {
      this.blockIfFull = blockIfFull;
    }

    public long getConnectionTimeoutMs() {
      return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
      this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public long getIdleTimeoutMs() {
      return idleTimeoutMs;
    }

    public void setIdleTimeoutMs(long idleTimeoutMs) {
      this.idleTimeoutMs = idleTimeoutMs;
    }

    public boolean isValidateOnBorrow() {
      return validateOnBorrow;
    }

    public void setValidateOnBorrow(boolean validateOnBorrow) {
      this.validateOnBorrow = validateOnBorrow;
    }

    public boolean isReconnectOnException() {
      return reconnectOnException;
    }

    public void setReconnectOnException(boolean reconnectOnException) {
      this.reconnectOnException = reconnectOnException;
    }
  }

  /**
   * Health check configuration.
   */
  public static class HealthProperties {

    private boolean enabled = true;

    private boolean validateQueues = true;

    private boolean validateChannels = true;

    @Min(0)
    private long timeoutMs = 5000L;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isValidateQueues() {
      return validateQueues;
    }

    public void setValidateQueues(boolean validateQueues) {
      this.validateQueues = validateQueues;
    }

    public boolean isValidateChannels() {
      return validateChannels;
    }

    public void setValidateChannels(boolean validateChannels) {
      this.validateChannels = validateChannels;
    }

    public long getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
      this.timeoutMs = timeoutMs;
    }
  }

  /**
   * Failure notification configuration.
   */
  public static class FailureProperties {

    private boolean publishEvents = true;

    private boolean logStructuredErrors = true;

    private boolean incrementMetrics = true;

    public boolean isPublishEvents() {
      return publishEvents;
    }

    public void setPublishEvents(boolean publishEvents) {
      this.publishEvents = publishEvents;
    }

    public boolean isLogStructuredErrors() {
      return logStructuredErrors;
    }

    public void setLogStructuredErrors(boolean logStructuredErrors) {
      this.logStructuredErrors = logStructuredErrors;
    }

    public boolean isIncrementMetrics() {
      return incrementMetrics;
    }

    public void setIncrementMetrics(boolean incrementMetrics) {
      this.incrementMetrics = incrementMetrics;
    }
  }

  /**
   * Observability configuration.
   */
  public static class ObservabilityProperties {

    private boolean metricsEnabled = true;

    private boolean structuredLoggingEnabled = true;

    private boolean correlationIdEnabled = true;

    private boolean openTelemetryEnabled = true;

    public boolean isMetricsEnabled() {
      return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
    }

    public boolean isStructuredLoggingEnabled() {
      return structuredLoggingEnabled;
    }

    public void setStructuredLoggingEnabled(boolean structuredLoggingEnabled) {
      this.structuredLoggingEnabled = structuredLoggingEnabled;
    }

    public boolean isCorrelationIdEnabled() {
      return correlationIdEnabled;
    }

    public void setCorrelationIdEnabled(boolean correlationIdEnabled) {
      this.correlationIdEnabled = correlationIdEnabled;
    }

    public boolean isOpenTelemetryEnabled() {
      return openTelemetryEnabled;
    }

    public void setOpenTelemetryEnabled(boolean openTelemetryEnabled) {
      this.openTelemetryEnabled = openTelemetryEnabled;
    }
  }
}
