/**
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Server for external site.
 */
var express = require('express'),
    fs = require('fs'),
    log4js = require('log4js'),
    cons = require('consolidate'),
    swig = require('swig'),
    request = require('request'),
    async = require('async'),
    argv = require('optimist').argv,
    nock = require('nock');

/**
 * Set environment vars.
 */
var env = argv.env || 'production';
var PORT = argv.port || 8100;
var CLIENT_ADDR = argv.loomhost || 'http://127.0.0.1:55054';
var BOX_ADDR = CLIENT_ADDR + '/v1/loom';
var CLIENT_DIR = env === 'production' ? 'client-built' : 'client';
console.info('Environment:', env, BOX_ADDR, CLIENT_DIR);

/**
 * If environment is test, use mock injector to simulate responses.
 */
if (env === 'test') {
  HttpMockInjector = require('./test/httpMockInjector');
  new HttpMockInjector(nock, argv, CLIENT_ADDR);
}

/**
 * Temporary login mocks.
 */
var ADMINS = {
  'admin': {
    username: 'admin',
    password: 'admin'
  }
};
var DEFAULT_API_KEY = '123456789abcdef';

/*
 * Configure logger for debugging.
 */
log4js.configure({
  appenders: [
    {type: 'console'}
  ]
});

/**
 * Set up site namespace.
 */
var site = site || {};

/**
 * Specify logger level.
 */
site.LOG_LEVEL = 'INFO';

/**
 * Attach logger to site.
 */
site.logger = log4js.getLogger('Loom');
site.logger.setLevel(site.LOG_LEVEL);

/*
 * Specify on which port server should run.
 */
site.PORT = PORT;

/**
 * Location of the template directory for swig templates.
 */
site.TEMPLATE_DIR = __dirname + '/templates';

/**
 * Max items to display per category on the home page.
 * @type {Number}
 */
site.HOME_MAX_ITEMS = 5;

/**
 * App framework.
 */
site.app = express();

/**
 * Temporary skins related data. Each server instance maintains a record of users
 * and their selected skins.
 */

site.skins = {};

/**
 * Configure static files server.
 */
site.app.use(express.cookieParser());
site.app.use(express.bodyParser());
site.app.use('/static', express.static(__dirname + '/' + CLIENT_DIR));

/**
 * Parses through each request and sets cookie.
 */
site.app.use(function (req, res, next) {

  site.TEMPLATE_DIR = __dirname + '/' + CLIENT_DIR + '/templates';
  site.configureTemplating(site.TEMPLATE_DIR);

  if (req.query.landing) {
    res.cookie('continuuity-landing', req.query.landing,
      { maxAge: 900000, httpOnly: false });
  }

  next();

});

/**
 * Configure templating engine.
 * @param {string} templateDir location of the template directory.
 */
site.configureTemplating = function(templateDir) {
  // Initialize swig.
  swig.init({
    root: templateDir,
    allowErrors: true
  });
  site.app.set('views', templateDir);
};

site.app.engine('.html', cons.swig);
site.app.set('view engine', 'html');
site.app.set('view options', { layout: false });

site.configureTemplating(site.TEMPLATE_DIR);

/**
 * Error handling middleware.
 */
site.app.use(express.methodOverride());
site.app.use(site.app.router);
site.app.use(function(err, req, res, next) {
  res.status(500);

  site.logger.error(err);

  // respond with html page
  if (req.accepts('html')) {
    res.render('500.html', {
      url: req.url,
      env: env,
      skin: site.getSkin()
    });
    return;
  }
  next();
});

/**
 * Gets data for a given restful url.
 * @param  {String} path Request path.
 * @param {String} user current logged in user id.
 */
site.getEntity = function (path, user) {
  return function (callback) {
    var options = {
      url: BOX_ADDR + path,
      method: 'GET',
      headers: {
        'X-Loom-UserID': user,
        'X-Loom-ApiKey': DEFAULT_API_KEY
      }
    };
    request(options, function (err, response, body) {
      if (err) {
        callback('Error: ' + JSON.stringify(err));
        return;  
      } else {
        if (body) {
          try {
            callback(null, JSON.parse(body));
          } catch (err) {
            callback(null, []);
          }
        } else {
          callback(null, []);
        }
        return;
      }
    });
  }
};

/**
 * Executes request for a given url and executes callback based on success or failure.
 * @param  {Object} options Request options object.
 * @param {String} user current logged in user id.
 * @param  {Object} res Response handler.
 */
site.sendRequestAndHandleResponse = function (options, user, res) {
  options['headers'] = {
    'X-Loom-UserID': user,
    'X-Loom-ApiKey': DEFAULT_API_KEY
  };
  request(options, this.getGenericResponseHandler(res, options.method));
};

/**
 * Sets up a generic response handler for an async http request and handles responses accordingly.
 * @param  {Object} res    Response handler.
 * @param  {String} method Method type.
 */
site.getGenericResponseHandler = function (res, method) {
  return function (err, response, body) {
    if (!err && response.statusCode == 200) {
      if (method === 'GET') {
        res.send(body);
      } else {
        res.send(response.statusCode);
      }
    } else {
      var respMessage = '';
      if (err) {
        respMessage += JSON.stringify(err);
      }
      respMessage += ' ' + body;
      res.send(respMessage);
    }
  }
};

/**
 * Verifies type of data passed in and passes default value otherwise.
 * @param  {Object|Array|String} arr input data
 * @return {Object|Array|String} formatted data.
 */
site.verifyData = function (arr) {
  if (arr && arr.length) {
    return arr;
  } else {
    return [];
  }
};

/**
 * Mock auth checker to sign each request and redirect the user.
 * !! This is not a real auth checker and can be easily fooled.
 * @param  {Boolean} admin Admin previlage required.
 * @return {Boolean} Whether user is authenticated.
 */
site.checkAuth = function (req, res, admin) {
  var authenticated = 'continuuity-loom-session' in req.cookies;
  if (!authenticated) {
    res.redirect('/login');
    return;
  }
  var auth = req.cookies['continuuity-loom-session'];
  if (!('user' in auth)) {
    res.redirect('/login');
    return;
  }
  var user = auth.user;
  if (admin) {
    if (user !== 'admin') {
      res.redirect('/login');
    }
  }
  return user;
};

/**
 * Determines permission level based on username and password.
 * @param  {String} username.
 * @param  {String} password.
 * @return {String} permission level.
 */
site.determinePermissionLevel = function (username, password) {
  var permissionLevel = 'user';
  for (item in ADMINS) {
    if (username === item) {
      if (ADMINS[item].password === password) {
        permissionLevel = 'admin';
        return permissionLevel;
      }
    }
  }
  return permissionLevel;
};

/**
 * Replaces date 
 * @param  {[type]} timestamp [description]
 * @return {[type]}           [description]
 */
site.formatDate = function (timestamp) {
  var dt = new Date(timestamp);
  return dt.toISOString().replace(/T/, ' ').replace(/\..+/, '');
};

/**
 * Parses cluster data and gets active and deleted clusters.
 * @param  {Array} clusters.
 * @return {Object} modified cluster data.
 */
site.parseClusterData = function (clusters) {
  var activeClusters = 0, deletedClusters = 0;
  var clusters = clusters.map(function (cluster) {
    if (cluster.createTime) {
      cluster.createTime = site.formatDate(cluster.createTime);
    }
    if (cluster.status !== 'TERMINATED') {
      activeClusters++;
    } else {
      deletedClusters++;
    }
    return cluster;
  });
  return {
    activeClusters: activeClusters,
    deletedClusters: deletedClusters,
    clusters: clusters
  };
};

/**
 * Get skin by username.
 * @param  {String} username.
 * @return {String} skin name.
 */
site.getSkin = function (username) {
  var selectedSkin = 'dark';
  if (username && username in site.skins) {
    selectedSkin = site.skins[username];
  }
  return selectedSkin;
};

/**
 * Pipes all frontend calls through to the loom server and returns responses. Expects path to come
 * in the form of query string after /v1/loom ex:
 * /v1/loom/providers => /pipeApiCall?path=/providers
 * /v1/loom/providers/rackspace => /pipeApiCall?path=/providers/rackspace
 */
site.app.get('/pipeApiCall', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + req.query.path,
    method: 'GET',

  };
  res.setHeader('Content-type', 'application/json');
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/import', function (req, res) {
  var user = site.checkAuth(req, res);
  if ('import-file' in req.files) {
    fs.readFile(req.files['import-file'].path, function (err, data) {
      async.parallel([
        site.getEntity('/clustertemplates', user),
        site.getEntity('/clusters', user),
      ], function (err, results) {
        var context = {
          authenticated: user,
          env: env,
          skin: site.getSkin(user)
        };
        if (err) {
          context.err = err;
        } else {
          context.clustertemplates = site.verifyData(results[0]);
          var clusters = results[1], activeNodes = 0, totalNodes = 0, totalClusters = 0,
          pendingClusters = 0;
          clusters.map(function (cluster) {
            if (cluster.status === 'ACTIVE') {
              activeNodes += cluster.numNodes;
              totalClusters++;
            }
            if (cluster.status === 'PENDING') {
              pendingClusters++;
            }
            totalNodes += cluster.numNodes;
          });
          context.pendingClusters = pendingClusters;
          context.totalNodes = totalNodes;
          context.activeNodes = activeNodes;
          context.totalClusters = totalClusters;
          var config;
          try {  
            config = JSON.parse(data);
          } catch (err) {
            context.err = "JSON parse error.";
            res.render('index.html', context);
            return;
          }
          var options = {
            url: BOX_ADDR + '/import',
            method: 'POST',
            headers: {
              'X-Loom-UserID': user,
              'X-Loom-ApiKey': DEFAULT_API_KEY
            },
            json: config
          };
          request(options, function (err, response, body) {
            if (!err && response.statusCode == 200) {
              res.redirect('/');
            } else {
              context.err = "Request could not be processed.";
              res.render('index.html', context);
              return;
            }
          });
        }
      });
    });
  } else {
    res.redirect('/error');
  }
});

site.app.get('/export', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    url: BOX_ADDR + '/export',
    method: 'GET',
    headers: {
      'X-Loom-UserID': user,
      'X-Loom-ApiKey': DEFAULT_API_KEY
    }
  };
  request(options, function (err, response, body) {
    if (!err && response.statusCode == 200) {
      res.setHeader('Content-disposition', 'attachment; filename=export.json');
      res.setHeader('Content-type', 'application/json');
      res.charset = 'UTF-8';
      res.write(body);
      res.end();
    } else {
      res.redirect('/error');
    }
  });
});

site.app.get('/', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/clustertemplates', user),
    site.getEntity('/clusters', user),
  ], function (err, results) {
    var context = {
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.clustertemplates = site.verifyData(results[0]);
      var clusters = results[1], activeNodes = 0, totalNodes = 0, totalClusters = 0,
      pendingClusters = 0;
      clusters.map(function (cluster) {
        if (cluster.status === 'ACTIVE') {
          activeNodes += cluster.numNodes;
          totalClusters++;
        }
        if (cluster.status === 'PENDING') {
          pendingClusters++;
        }
        totalNodes += cluster.numNodes;
      });
      context.pendingClusters = pendingClusters;
      context.totalNodes = totalNodes;
      context.activeNodes = activeNodes;
      context.totalClusters = totalClusters;
    }
    res.render('index.html', context);
  });
});

site.app.get('/profile', function (req, res) {
  var user = site.checkAuth(req, res, false);
  var context = {
    authenticated: user,
    env: env,
    skin: site.getSkin(user)
  };
  res.render('profile.html', context);
});

site.app.post('/setskin', function (req, res) {
  var user = site.checkAuth(req, res, false);
  if ('skin' in req.body) {
    site.skins[user] = req.body.skin;
  }
  res.redirect('/profile');
});

site.app.get('/clustertemplates', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/clustertemplates', user)
  ], function (err, results) {
    var context = {
      activeTab: 'clustertemplates',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)  
    };
    if (err) {
      context.err = err;
    } else {
      context.clustertemplates = site.verifyData(results[0]);
    }
    res.render('clustertemplates/clustertemplates.html', context);
  });
});

site.app.get('/clustertemplates/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/providers', user),
    site.getEntity('/hardwaretypes', user),
    site.getEntity('/imagetypes', user),
    site.getEntity('/services', user),
    site.getEntity('/clustertemplates', user)
  ], function (err, results) {
    var context = {
      activeTab: 'clustertemplates',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.providers = site.verifyData(results[0]);
      context.hardwaretypes = site.verifyData(results[1]);
      context.imagetypes = site.verifyData(results[2]);
      context.services = site.verifyData(results[3]);
      context.clustertemplates = site.verifyData(results[4]);
    }
    res.render('clustertemplates/createclustertemplate.html', context);
  });
});

site.app.post('/clustertemplates/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/clustertemplates',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/clustertemplates/update', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/clustertemplates/' + req.body.name,
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/clustertemplates/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clustertemplates/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});


site.app.get('/clustertemplates/clustertemplate/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var clustertemplateId = req.params.id;
  async.parallel([
    site.getEntity('/clustertemplates', user),
    site.getEntity('/providers', user),
    site.getEntity('/hardwaretypes', user),
    site.getEntity('/imagetypes', user),
    site.getEntity('/services', user)
  ], function (err, results) {
    var context = {
      activeTab: 'clustertemplates',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.clustertemplates = site.verifyData(results[0]);
      context.clustertemplateId = clustertemplateId;
      context.providers = site.verifyData(results[2]);
      context.hardwaretypes = site.verifyData(results[3]);
      context.imagetypes = site.verifyData(results[4]);
      context.services = site.verifyData(results[5]);
    }
    res.render('clustertemplates/createclustertemplate.html', context);
  });
});

site.app.get('/hardwaretypes', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/hardwaretypes', user)
  ], function (err, results) {
    var context = {
      activeTab: 'hardwaretypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.hardwaretypes = site.verifyData(results[0]);
    }
    res.render('hardwaretypes/hardwaretypes.html', context);
  });
});

site.app.get('/hardwaretypes/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/hardwaretypes', user),
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'hardwaretypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.hardwaretypes = site.verifyData(results[0]);
      context.providers = site.verifyData(results[1]);
    }
    res.render('hardwaretypes/createhardwaretype.html', context);
  });
});

site.app.post('/hardwaretypes/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/hardwaretypes',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/hardwaretypes/update', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/hardwaretypes/' + req.body.name,
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/hardwaretypes/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/hardwaretypes/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});


site.app.get('/hardwaretypes/hardwaretype/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var hardwaretypeId = req.params.id;
  async.parallel([
    site.getEntity('/hardwaretypes', user),
    site.getEntity('/hardwaretypes/' + hardwaretypeId, user),
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'hardwaretypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.hardwaretypes = site.verifyData(results[0]);
      context.hardwaretype = results[1];
      context.providers = site.verifyData(results[2]);
    }
    res.render('hardwaretypes/createhardwaretype.html', context);
  });
});

site.app.get('/imagetypes', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/imagetypes', user)
  ], function (err, results) {
    var context = {
      activeTab: 'imagetypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.imagetypes = site.verifyData(results[0]);
    }
    res.render('imagetypes/imagetypes.html', context);
  });
});

site.app.get('/imagetypes/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/imagetypes', user),
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'imagetypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.imagetypes = site.verifyData(results[0]);
      context.providers = site.verifyData(results[1]);
    }
    res.render('imagetypes/createimagetype.html', context);
  });
});

site.app.post('/imagetypes/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/imagetypes',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/imagetypes/update', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/imagetypes/' + req.body.name,
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/imagetypes/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/imagetypes/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});


site.app.get('/imagetypes/imagetype/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var imagetypeId = req.params.id;
  async.parallel([
    site.getEntity('/imagetypes', user),
    site.getEntity('/imagetypes/' + imagetypeId, user),
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'imagetypes',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.imagetypes = site.verifyData(results[0]);
      context.imagetype = results[1];
      context.providers = site.verifyData(results[2]);
    }
    res.render('imagetypes/createimagetype.html', context);
  });
});

site.app.get('/providers', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'providers',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      var providers = site.verifyData(results[0]);
      for (var i = 0; i < providers.length; i++) {
        providers[i].provisioner.auth = JSON.stringify(providers[i].provisioner.auth)
      }
      context.providers = providers;
    }
    res.render('providers/providers.html', context);
  });
});

site.app.get('/providers/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/providers', user)
  ], function (err, results) {
    var context = {
      activeTab: 'providers',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.providers = site.verifyData(results[0]);
    }
    res.render('providers/providerbase.html', context);
  });
});

site.app.post('/providers/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/providers',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/providers/update', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/providers/' + req.body.name,
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/providers/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var providerId = req.params.id;
  var options = {
    uri: BOX_ADDR + '/providers/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});


site.app.get('/providers/provider/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var providerId = req.params.id;
  async.parallel([
    site.getEntity('/providers', user),
    site.getEntity('/providers/' + providerId, user)
  ], function (err, results) {
    var context = {
      activeTab: 'providers',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.providers = site.verifyData(results[0]);
      context.provider = results[1];
    }
    res.render('providers/providerbase.html', context);
  });
});

site.app.get('/services', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/services', user)
  ], function (err, results) {
    var context = {
      activeTab: 'services',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.services = site.verifyData(results[0]);
    }
    res.render('services/services.html', context);
  });
});

site.app.get('/services/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/services', user)
  ], function (err, results) {
    var context = {
      activeTab: 'services',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.services = site.verifyData(results[0]);
    }
    res.render('services/createservice.html', context);
  });
});

site.app.post('/services/create', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/services',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/services/update', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/services/' + req.body.name,
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/services/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var options = {
    uri: BOX_ADDR + '/services/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});


site.app.get('/services/service/:id', function (req, res) {
  var user = site.checkAuth(req, res, true);
  var serviceId = req.params.id;
  async.parallel([
    site.getEntity('/services', user),
  ], function (err, results) {
    res.render('services/createservice.html', {
      services: results[0],
      serviceId: serviceId,
      activeTab: 'services',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    });
  });
});

site.app.get('/admin/clusters', function (req, res) {
  var user = site.checkAuth(req, res, true);
  async.parallel([
    site.getEntity('/clusters', user),
  ], function (err, results) {
    var context = {
      activeTab: 'clusters',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      var clusterData = site.parseClusterData(results[0]);
      context.activeClusters = clusterData['activeClusters'];
      context.deletedClusters = clusterData['deletedClusters'];
      context.clusters = clusterData['clusters'];
    }
    res.render('clusters/clusters.html', context);
  });
});

site.app.get('/user', function (req, res) {
  res.redirect('/user/clusters');
});

site.app.get('/user/clusters', function (req, res) {
  var user = site.checkAuth(req, res);
  async.parallel([
    site.getEntity('/clusters', user),
  ], function (err, results) {
    var context = {
      activeTab: 'clusters',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      var clusterData = site.parseClusterData(results[0]);
      context.activeClusters = clusterData['activeClusters'];
      context.deletedClusters = clusterData['deletedClusters'];
      context.clusters = clusterData['clusters'];
    }
    res.render('user/clusters/clusters.html', context);
  });
});

site.app.get('/user/clusters/cluster/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var clusterId = req.params.id;
  async.parallel([
    site.getEntity('/clusters', user),
  ], function (err, results) {
    var context = {
      activeTab: 'clusters',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.clusters = results[0];
      context.clusterId = clusterId;
    }
    res.render('user/clusters/describecluster.html', context);
  });
});

site.app.get('/user/clusters/cluster/:id/reconfigure', function (req, res) {
  var user = site.checkAuth(req, res);
  var clusterId = req.params.id;
  async.parallel([
    site.getEntity('/clusters', user),
    site.getEntity('/clustertemplates', user)
  ], function (err, results) {
    var context = {
      activeTab: 'clusters',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.clusters = results[0];
      context.clustertemplates = site.verifyData(results[1]);
      context.clusterId = clusterId;
    }
    res.render('user/clusters/createcluster.html', context);
  });
});

site.app.post('/user/clusters/cluster/:id/reconfigure', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/config',
    method: 'PUT',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/cluster/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id,
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/cluster/:id/services', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/services',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/cluster/:id/services/:serviceid/start', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/services/' + req.params.serviceid + '/start',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/cluster/:id/services/:serviceid/stop', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/services/' + req.params.serviceid + '/stop',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/cluster/:id/services/:serviceid/restart', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/services/' + req.params.serviceid + '/restart',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.get('/user/clusters/create', function (req, res) {
  var user = site.checkAuth(req, res);
  async.parallel([
    site.getEntity('/clusters', user),
    site.getEntity('/clustertemplates', user)
  ], function (err, results) {
    var context = {
      activeTab: 'clusters',
      authenticated: user,
      env: env,
      skin: site.getSkin(user)
    };
    if (err) {
      context.err = err;
    } else {
      context.clusters = results[0];
      context.clustertemplates = site.verifyData(results[1]);
    }
    res.render('user/clusters/createcluster.html', context);
  });
});

site.app.post('/user/clusters/create', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters',
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/update/:clusterId', function (req, res) {
  var clusterId = req.params.clusterId;
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + clusterId,
    method: 'POST',
    json: req.body
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/delete/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id,
    method: 'DELETE'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.post('/user/clusters/abort/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/abort',
    method: 'POST'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

site.app.get('/user/clusters/status/:id', function (req, res) {
  var user = site.checkAuth(req, res);
  var options = {
    uri: BOX_ADDR + '/clusters/' + req.params.id + '/status',
    method: 'GET'
  };
  site.sendRequestAndHandleResponse(options, user, res);
});

/**
 * Login/logout routes. Post does the logging in and sets mock auth via a cookie.
 * !This is not a real auth system and can be easily duped, replace completely when appropriate.
 */
site.app.get('/login', function (req, res) {
  res.clearCookie('continuuity-loom-session');
  var authenticated = false;
  res.render('login.html', {
    authenticated: authenticated,
    env: env,
    skin: site.getSkin()
  });
});

site.app.post('/login', function (req, res) {
  if (!('username' in req.body) || !('password' in req.body)) {
    res.redirect('/login');
    return;
  }
  var user = req.body.username;
  var password = req.body.password;
  if (user === 'admin' && password != ADMINS.admin.password) {
    res.redirect('/login');
    return;
  }
  var permissionLevel = site.determinePermissionLevel(user, password);
  res.cookie('continuuity-loom-session', { user: user, permission: permissionLevel });
  var authenticated = true;
  if (permissionLevel === 'admin') {
    res.redirect('/');
  } else {
    res.redirect('/user');
  }
});

/**
 * Status of application.
 */
site.app.get('/status', function (req, res) {
  res.send('OK');
});

site.app.get('/error', function (req, res) {
  res.render('404.html', {
    url: req.url,
    env: env,
    skin: site.getSkin()
  });
  return;
});

/**
 * Blanket router for all unhandled routes.
 */
site.app.get('/*', function(req, res) {

  var page = req.originalUrl.slice(1).split('?')[0];
  page = page.substr(-1) == '/' ? page.substr(0, page.length - 1) : page;
  res.status(404);

  // respond with html page
  if (req.accepts('html')) {
    res.render('404.html', {
      url: req.url,
      env: env,
      skin: site.getSkin()
    });
    return;
  }

});

/**
 * Send last route for a page not found page.
 * !!!Keep this as the last route on the page.
 */
site.app.use(function (req, res, next) {
  res.status(404);

  // respond with html page
  if (req.accepts('html')) {
    res.render('404.html', {
      url: req.url,
      env: env,
      skin: site.getSkin()
    });
    return;
  }
  next();
});

/*
 * Start express server on specified port and show message if successful.
 */
site.app.listen(site.PORT);
site.logger.info('Server started on port ', site.PORT);

/**
 * Export module.
 */
module.exports = site;
