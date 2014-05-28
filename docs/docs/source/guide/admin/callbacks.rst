..
   Copyright 2012-2014, Continuuity, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

:orphan:

.. callbacks-reference:

.. index::
   single: Cluster Callbacks

=================
Cluster Callbacks
=================

.. include:: /guide/admin/admin-links.rst

Overview
--------

The Server can perform custom callbacks before the start of any cluster operation, after an operation has successfully
completed, and after an operation has failed. To activate callbacks, you must set the ``loom.callback.class`` setting
in the server config to the fully qualified name of the class you want to use. The class must implement the 
``ClusterCallback`` interface, and the jar containing the class must be included in the lib directory for the Server. 

Interface
---------
.. code-block:: java 

  /**
   * Executes some code before a job starts and after a job completes. Callbacks must be idempotent. There is a
   * possibility they get called more than once if the server goes down at the right time.
   */
  public interface ClusterCallback {

    /**
     * Initialize the cluster callback. Guaranteed to be called exactly once before any other methods are called.
     *
     * @param conf Server configuration.
     * @param clusterStore Cluster store for looking up cluster information.
     */
    void initialize(Configuration conf, ClusterStore clusterStore);

    /**
     * Execute some method before a cluster job starts, returning whether or not the job can proceed or whether it should
     * be failed. Is guaranteed to be called and executed before the cluster operation begins.
     *
     * @param data Data available to use while executing callback.
     * @return True if it is ok to proceed with the operation, false if not.
     */
    boolean onStart(CallbackData data);

    /**
     * Execute some method after a cluster completes successfully.
     *
     * @param data Data available to use while executing callback.
     */
    void onSuccess(CallbackData data);

    /**
     * Execute some method after a cluster job fails.
     *
     * @param data Data available to use while executing callback.
     */
    void onFailure(CallbackData data);
  }

The initialize method is called once when the Server starts up. It will be called before any other method is called
and should be used to initialize any data needed by the callback. The configuration object passed in provides access
to any settings set in the server config, allowing callback authors to define their own configuration settings.  
The onStart method is called before execution of a cluster operation begins. It returns a boolean, which indicates
whether or not the cluster operation can proceed. The onSuccess method is called on success of a cluster operation,
and the onFailure method is called on failure of a cluster operation. CallbackData contains the cluster object and
cluster job that is about to be executed or that has already been executed. 

Example
-------

The Server comes with a HttpPostRequestCallback class that can send a HTTP POST request containing the cluster, nodes,
and job to a configurable URL at start, success, and failure of different types of cluster operations. It is shown here
as an example of how one could write a callback class.

.. code-block:: java

  /**
   * Executes before and after hooks by sending an HTTP POST request to some configurable endpoints, with the post body
   * containing the cluster and job objects, assuming there is a valid url assigned to the start, success, and/or failure
   * urls. If no url is specified, no request will be sent. Additionally, trigger actions can be configured so that
   * the HTTP POST request is sent only for specific cluster actions. This is done by specifying a comma separated list
   * of {@link ClusterAction}s in the configuration for start, success, and/or triggers.
   */
  public class HttpPostClusterCallback implements ClusterCallback {
    private static final Logger LOG = LoggerFactory.getLogger(HttpPostClusterCallback.class);
    private static final Gson GSON = new JsonSerde().getGson();
    private String onStartUrl;
    private String onSuccessUrl;
    private String onFailureUrl;
    private Set<ClusterAction> startTriggerActions;
    private Set<ClusterAction> successTriggerActions;
    private Set<ClusterAction> failureTriggerActions;
    private HttpClient httpClient;
    private ClusterStore clusterStore;

    public void initialize(Configuration conf, ClusterStore clusterStore) {
      this.clusterStore = clusterStore;
      this.onStartUrl = conf.get(Constants.HttpCallback.START_URL);
      this.onSuccessUrl = conf.get(Constants.HttpCallback.SUCCESS_URL);
      this.onFailureUrl = conf.get(Constants.HttpCallback.FAILURE_URL);
      this.startTriggerActions = parseActionsString(conf.get(Constants.HttpCallback.START_TRIGGERS,
                                                              Constants.HttpCallback.DEFAULT_START_TRIGGERS));
      this.successTriggerActions = parseActionsString(conf.get(Constants.HttpCallback.SUCCESS_TRIGGERS,
                                                             Constants.HttpCallback.DEFAULT_SUCCESS_TRIGGERS));
      this.failureTriggerActions = parseActionsString(conf.get(Constants.HttpCallback.FAILURE_TRIGGERS,
                                                               Constants.HttpCallback.DEFAULT_FAILURE_TRIGGERS));

      int maxConnections = conf.getInt(Constants.HttpCallback.MAX_CONNECTIONS,
                                       Constants.HttpCallback.DEFAULT_MAX_CONNECTIONS);
      PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
      connectionManager.setDefaultMaxPerRoute(maxConnections);
      connectionManager.setMaxTotal(maxConnections);

      SocketConfig socketConfig = SocketConfig.custom()
        .setSoTimeout(conf.getInt(Constants.HttpCallback.SOCKET_TIMEOUT,
                                  Constants.HttpCallback.DEFAULT_SOCKET_TIMEOUT))
        .build();
      connectionManager.setDefaultSocketConfig(socketConfig);
      this.httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
    }

    private Set<ClusterAction> parseActionsString(String actionsStr) {
      if (actionsStr == null) {
        return ImmutableSet.of();
      }

      Iterator<String> actionIter = Splitter.on(',').split(actionsStr).iterator();
      Set<ClusterAction> actions = Sets.newHashSet();
      while (actionIter.hasNext()) {
        String actionStr = actionIter.next();
        try {
          ClusterAction action = ClusterAction.valueOf(actionStr.toUpperCase());
          actions.add(action);
        } catch (IllegalArgumentException e) {
          LOG.warn("Unknown cluster action " + actionStr + ". Hooks will not be executed for that action");
        }
      }
      return actions;
    }

    public boolean onStart(CallbackData data) {
      ClusterAction jobAction = data.getJob().getClusterAction();
      if (startTriggerActions.contains(jobAction)) {
        sendPost(onStartUrl, data);
      }
      return true;
    }

    public void onSuccess(CallbackData data) {
      ClusterAction jobAction = data.getJob().getClusterAction();
      if (successTriggerActions.contains(data.getJob().getClusterAction())) {
        sendPost(onSuccessUrl, data);
      }
    }

    @Override
    public void onFailure(CallbackData data) {
      ClusterAction jobAction = data.getJob().getClusterAction();
      if (failureTriggerActions.contains(data.getJob().getClusterAction())) {
        sendPost(onFailureUrl, data);
      }
    }

    private void sendPost(String url, CallbackData data) {
      if (url != null) {
        HttpPost post = new HttpPost(url);
        Set<Node> nodes = null;
        try {
          nodes = clusterStore.getClusterNodes(data.getCluster().getId());
        } catch (Exception e) {
          LOG.error("Unable to fetch nodes for cluster {}, not sending post request.", data.getCluster().getId());
          return;
        }

        try {
          JsonObject body = new JsonObject();
          body.add("cluster", GSON.toJsonTree(data.getCluster()));
          body.add("job", GSON.toJsonTree(data.getJob()));
          body.add("nodes", GSON.toJsonTree(nodes));
          post.setEntity(new StringEntity(GSON.toJson(body)));
          httpClient.execute(post);
        } catch (UnsupportedEncodingException e) {
          LOG.warn("Exception setting http post body", e);
        } catch (ClientProtocolException e) {
          LOG.warn("Exception executing http post callback to " + url, e);
        } catch (IOException e) {
          LOG.warn("Exception executing http post callback to " + url, e);
        } catch (Exception e) {
          LOG.warn("Exception executing http post callback to " + url, e);
        } finally {
          post.releaseConnection();
        }
      }
    }
  }
