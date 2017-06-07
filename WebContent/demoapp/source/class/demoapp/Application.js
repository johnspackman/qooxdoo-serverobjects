/* ************************************************************************

   server-objects - a contrib to the Qooxdoo project (http://qooxdoo.org/)

   http://qooxdoo.org

   Copyright:
     2010 Zenesis Limited, http://www.zenesis.com

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     
     This software is provided under the same licensing terms as Qooxdoo,
     please see the LICENSE file in the Qooxdoo project's top-level directory 
     for details.

   Authors:
 * John Spackman (john.spackman@zenesis.com)

 ************************************************************************ */

/**
 * This is the main application class of your custom application "demoapp"
 * 
 * @require(qx.Promise)
 * @asset(demoapp/*)
 * @ignore(com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer)
 * @ignore(com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child)
 * @ignore(com.zenesis.qx.remote.test.simple.Pippo)
 * @ignore(com.zenesis.qx.remote.test.properties.TestProperties)
 * 
 */
qx.Class.define("demoapp.Application", {
  extend: qx.application.Standalone,
  include: [qx.core.MAssert],

  /*
   * ****************************************************************************
   * MEMBERS
   * ****************************************************************************
   */

  members: {
    /**
     * This method contains the initial application code and gets called during
     * startup of the application
     * 
     * @ignore(alert)
     * @ignore(com.zenesis.qx.remote.test.simple.Pippo)
     * @ignore(com.zenesis.qx.remote.test.properties.TestProperties)
     */
    main: function() {
      var t = this;

      this.base(arguments);

      qx.log.appender.Native;

      /*
       * -------------------------------------------------------------------------
       * Below is your actual application code...
       * -------------------------------------------------------------------------
       */

      var manager = new com.zenesis.qx.remote.ProxyManager("/sampleServlet/ajax", true);
      manager.setTimeout(120 * 60 * 1000);

      var root = this.getRoot();
      var txtLog = this.__txtLog = new qx.ui.form.TextArea().set({ readOnly: true, minHeight: 400 });
      root.add(txtLog, { left: 0, right: 0, bottom: 0 });
      
      this.log("Testing queued async");
      this.testQueuedAsyncMethods(function() {
        
        // Using the LogAppender can cause extra server round trips which get in the way of testQueuedAsyncMethods
        //   because it counts the number of server trips to check everything is working as expected, so we don't
        //  initialise it until here
        com.zenesis.qx.remote.LogAppender.install();
        qx.event.GlobalError.setErrorHandler(function(ex) {
          t.error("Unhandled error: " + ex.stack);
        }, t);
        
        t.log("Testing main");
        t.testMain();
        
        t.log("Testing ArrayLists");
        t.testArrayLists();
        
        t.log("Running testRecursiveArray");
        t.testRecursiveArray();
        
        t.log("Testing Maps");
        new demoapp.test.DemoTest().testMap();
        t.testMaps();
       
        t.log("Running testObjectKeyMaps");
        t.testObjectKeyMaps();

        t.log("All automated tests passed - now open other browsers to start multi user testing");
        t.testMultiUser();
        t.testThreading();
        
        t.testBrowserTimeouts();
      });
    },
    
    log: function(msg) {
      var txtLog = this.__txtLog;
      var str = txtLog.getValue()||"";
      str += msg + "\n";
      txtLog.setValue(str);
    },

    
    testMain: function() {
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var boot = manager.getBootstrapObject();
      var mainTests = boot.getMainTests();

      var dt = mainTests.getTodaysDate();
      dt.setDate(dt.getDate() - 1);
      qx.core.Assert.assertTrue(mainTests.isYesterday(dt), "Dates are not passed correctly");

      qx.core.Assert.assertTrue(mainTests.constructor.myStaticMethod("hello") === "static+hello", "static methods not working");

      var cont = new com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer();
      cont.set({
        list: new qx.data.Array(),
        map: new com.zenesis.qx.remote.Map()
      });
      cont.getList().push(new com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child().set({
        name: "alpha"
      }));
      cont.getList().push(new com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child().set({
        name: "bravo"
      }));
      cont.getList().push(new com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child().set({
        name: "charlie"
      }));
      cont.getMap().put("alpha", "one");
      cont.getMap().put("bravo", "two");
      cont.getMap().put("charlie", "three");
      cont.test();

      var pippo1 = new com.zenesis.qx.remote.test.simple.Pippo();
      var result = pippo1.getExampleCode();
      for (var i = 0; i < result.getLength(); i++) {
        this.debug("Pippo #" + i + ": name=" + result.getItem(i).getName());
      }
      qx.core.Assert.assertEquals(2, result.getLength());
      qx.core.Assert.assertEquals("prova1", result.getItem(0).getName());
      qx.core.Assert.assertEquals("prova2", result.getItem(1).getName());

      var pippo2 = new com.zenesis.qx.remote.test.simple.Pippo();
      pippo1.setName("hello");
      pippo2.setName("world");
      var result = mainTests.testPippoArray([ pippo1, pippo2 ]);
      this.debug("testPippoArray: " + result);
      qx.core.Assert.assertEquals("Pippo #0: name=helloPippo #1: name=world", result);
      var testScalars = mainTests.getTestScalars();
      
      var testAnnos = mainTests.getTestAnnos();
      function getPropertyAnno(name, clazz) {
        var annos = qx.Annotation.getProperty(testAnnos.constructor, name);
        for (var i = 0; i < annos.length; i++) {
          if (annos[i] instanceof clazz)
            return annos[i];
        }
        return null;
      }
      qx.core.Assert.assertArrayEquals(["qso.test.myAnno"], qx.Annotation.getProperty(testAnnos.constructor, "test"));
      qx.core.Assert.assertArrayEquals(["qso.test.myMethodAnno"], qx.Annotation.getMember(testAnnos.constructor, "helloWorld"));
      qx.core.Assert.assertEquals("String", getPropertyAnno("myStrings", com.zenesis.qx.remote.annotations.Property).getComponentTypeName());
      qx.core.Assert.assertEquals("com.zenesis.qx.remote.test.simple.TestAnnos", getPropertyAnno("myTestAnnos", com.zenesis.qx.remote.annotations.Property).getComponentTypeName());
      qx.core.Assert.assertEquals("String", getPropertyAnno("myTestAnnosMap", com.zenesis.qx.remote.annotations.Property).getKeyTypeName());
      qx.core.Assert.assertEquals("com.zenesis.qx.remote.test.simple.TestAnnos", getPropertyAnno("myTestAnnosMap", com.zenesis.qx.remote.annotations.Property).getComponentTypeName());

      mainTests.waitForMillis(1000, function(result) {
        this.debug("waitForMillis completed, result=" + result);
        qx.core.Assert.assertTrue(this == mainTests);
        qx.core.Assert.assertTrue(result == 1000);
      });

      mainTests.waitForMillis(250, function(result) {
        this.debug("waitForMillis completed, result=" + result);
        qx.core.Assert.assertTrue(this == mainTests);
        qx.core.Assert.assertTrue(result == 250);
      });

      mainTests.waitForMillis(2000, function(result) {
        this.debug("waitForMillis completed, result=" + result);
        qx.core.Assert.assertTrue(this == mainTests);
        qx.core.Assert.assertTrue(result == 2000);
      });

      qx.core.Assert.assertTrue(testScalars.getZero() === 0);
      qx.core.Assert.assertTrue(testScalars.getTrue() === true);
      qx.core.Assert.assertTrue(testScalars.getFalse() === false);
      qx.core.Assert.assertTrue(testScalars.getNullBoolean() === null);
      qx.core.Assert.assertTrue(testScalars.getNullBooleanProperty() === null);
      qx.core.Assert.assertEquals(43, testScalars.getFourtyThree());
      qx.core.Assert.assertEquals(6.7, testScalars.getSixPointSeven());
      qx.core.Assert.assertEquals("Hello World", testScalars.getHelloWorld());
      var names = testScalars.getNames();
      var str = "";
      for (var i = 0; i < names.length; i++) {
        if (i > 0)
          str += ",";
        str += names[i];
      }
      qx.core.Assert.assertEquals("Jack,Jill,Bill,Ben", str);
      qx.core.Assert.assertEquals(25, testScalars.addUp([ 1, 3, 5, 7, 9 ]));
      qx.core.Assert.assertTrue(mainTests.verifyTestScalars(testScalars));

      var tp = mainTests.getTestProperties();
      var numCalls = manager.getNumberOfCalls();
      str = tp.getQueued();
      qx.core.Assert.assertEquals("Server Queued", str);
      tp.setQueued("queued from client");
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      tp.setImmediate("immediate from client");
      numCalls++;
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());

      var tg = mainTests.getTestGroups();
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals(tg.getAlpha(), "Alpha");
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals(tg.getBravo(), "Bravo");
      numCalls++;
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals(tg.getCharlie(), "Charlie");
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals(tg.getDelta(), "Delta");
      numCalls++;
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals(tg.getEcho(), "Echo");
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());

      str = tp.getChangeLog();
      this.debug("tp.changeLog = " + str);

      var numChangeDemandString = 0;
      tp.addListener("changeDemandString", function(evt) {
        numChangeDemandString++;
        qx.core.Assert.assertEquals("Hello World", evt.getData());
        qx.core.Assert.assertEquals("MyOnDemandString", evt.getOldData());
      }, this);
      numCalls = manager.getNumberOfCalls();
      qx.core.Assert.assertEquals("MyOnDemandString", tp.getOnDemandString());
      qx.core.Assert.assertEquals(numCalls + 1, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals("MyOnDemandString", tp.getOnDemandString());
      qx.core.Assert.assertEquals(numCalls + 1, manager.getNumberOfCalls());
      qx.core.Assert.assertEquals("MyOnDemandPreload", tp.getOnDemandPreload());
      qx.core.Assert.assertEquals(numCalls + 1, manager.getNumberOfCalls());

      tp.setOnDemandString("Hello World");
      qx.core.Assert.assertEquals(numChangeDemandString, 1);
      qx.core.Assert.assertEquals("Hello World", tp.getOnDemandString());
      qx.core.Assert.assertEquals(numCalls + 1, manager.getNumberOfCalls());

      var watchedStringA = "unknown";
      var watchedChangedA = 0;
      tp.addListener("changeWatchedString", function(evt) {
        watchedStringA = evt.getData();
        watchedChangedA++;
      }, this);
      var watchedStringB = "unknown";
      var watchedChangedB = 0;
      tp.addListener("changeWatchedString", function(evt) {
        watchedStringB = evt.getData();
        watchedChangedB++;
      }, this);
      tp.triggerChangeWatchedString();
      qx.core.Assert.assertEquals(watchedChangedA, 1);
      qx.core.Assert.assertEquals(watchedStringA, "Watched=1");
      qx.core.Assert.assertEquals(watchedChangedB, 1);
      qx.core.Assert.assertEquals(watchedStringB, "Watched=1");
      tp.triggerChangeWatchedString();
      qx.core.Assert.assertEquals(watchedChangedA, 2);
      qx.core.Assert.assertEquals(watchedStringA, "Watched=2");
      qx.core.Assert.assertEquals(watchedChangedB, 2);
      qx.core.Assert.assertEquals(watchedStringB, "Watched=2");

      var someEventFires = 0;
      tp.addListener("someEvent", function(evt) {
        someEventFires++;
      }, this);
      tp.triggerSomeEvent();
      qx.core.Assert.assertEquals(someEventFires, 1);
      tp.triggerSomeEvent();
      qx.core.Assert.assertEquals(someEventFires, 2);

      numCalls = manager.getNumberOfCalls();
      var myTp = new com.zenesis.qx.remote.test.properties.TestProperties();
      myTp.setWatchedString("setByClientMethod");
      qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
      qx.core.Assert.assertTrue(mainTests.checkNewTestProperties(myTp));
      qx.core.Assert.assertEquals(numCalls + 1, manager.getNumberOfCalls());

      var myTp = new com.zenesis.qx.remote.test.properties.TestProperties();
      myTp.setWatchedString("setByClientProperty");
      mainTests.setClientTestProperties(myTp);
      mainTests.checkClientTestProperties();

      var testEx = mainTests.getTestExceptions();
      var str = testEx.getString();
      try {
        testEx.setString("my client string");
      } catch (ex) {
      }
      qx.core.Assert.assertEquals(str, testEx.getString());

      try {
        testEx.throwException();
        qx.core.Assert.assertTrue(false);
      } catch (ex) {
        this.debug("Caught exception: " + ex);
      }

      var testArr = mainTests.getTestArrays();
      var tmp = testArr.getScalarArray();
      qx.core.Assert.assertTrue(qx.Class.isSubClassOf(tmp.constructor, qx.data.Array), "Expecting instance of qx.data.Array, not "
          + tmp.constructor);
      qx.core.Assert.assertArrayEquals([ "One", "Two", "Three", "Four", "Five" ], tmp.toArray());
      tmp.sort();
      qx.core.Assert.assertTrue(testArr.testScalarArray(tmp.toArray()),
          "testScalarArray failed - the array has not been updated properly");

      var tmp = testArr.getScalarArrayList();
      qx.core.Assert.assertTrue(qx.Class.isSubClassOf(tmp.constructor, qx.data.Array), "Expecting instance of qx.data.Array, not "
          + tmp.constructor);
      qx.core.Assert.assertArrayEquals([ "One", "Two", "Three", "Four", "Five" ], tmp.toArray());
      tmp.sort();
      qx.core.Assert.assertTrue(testArr.testScalarArrayList(tmp.toArray()),
          "testScalarArrayList failed - the array has not been updated properly");

      var tmp = testArr.getObjectArray();
      qx.core.Assert.assertTrue(qx.Class.isSubClassOf(tmp.constructor, qx.data.Array), "Expecting instance of qx.data.Array, not "
          + tmp.constructor);
      for (var i = 0; i < 5; i++)
        qx.core.Assert.assertEquals(tmp.getItem(i).getValue(), i + 1);
      tmp.sort();
      qx.core.Assert.assertTrue(testArr.testObjectArray(tmp.toArray()),
          "testObjectArray failed - the array has not been updated properly");

      var tmp = testArr.getObjectArrayList();
      qx.core.Assert.assertTrue(qx.Class.isSubClassOf(tmp.constructor, qx.data.Array), "Expecting instance of qx.data.Array, not "
          + tmp.constructor);
      for (var i = 0; i < 5; i++)
        qx.core.Assert.assertEquals(tmp.getItem(i).getValue(), i + 1);
      tmp.sort();
      qx.core.Assert.assertTrue(testArr.testObjectArrayList(tmp.toArray()),
          "testObjectArrayList failed - the array has not been updated properly");

      tmp = testArr.getReadOnlyArray();
      tmp.splice(1, 1, "stuff");
      qx.core.Assert.assertTrue(testArr.checkReadOnlyArray(), "read only array is not read only");

      tmp = mainTests.getTestMap();

      var map = tmp.getWrappedStringMap();
      qx.core.Assert.assertEquals("one", map.get("alpha"));
      map.remove("bravo");
      map.put("charlie", "three-changed");
      map.put("delta", "four");
      tmp.checkMapUpdated();

      map = tmp.getUnwrappedStringMap();
      qx.core.Assert.assertEquals("one", map.alpha);

      map = tmp.getWrappedStringMapMethod();
      qx.core.Assert.assertEquals("one", map.get("alpha"));

      map = tmp.getObjectMap();
      qx.core.Assert.assertTrue(!!map.get("alpha"));
      qx.core.Assert.assertEquals("com.zenesis.qx.remote.test.collections.TestJavaUtilMap", map.get("alpha").classname);
      map.put("bravo", map.get("alpha"));
      map.remove("alpha");
      tmp.checkObjectMap();

      map = tmp.getEnumMap();
      map.remove("alpha");
      map.put("charlie", "three");
      tmp.checkEnumMap();
      
    },
    
    testQueuedAsyncMethods: function(cb) {
      var t = this;
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var boot = manager.getBootstrapObject();
      var threadTest = boot.getThreadTest();
      
      function makeSimple(value) {
        return new com.zenesis.qx.remote.test.properties.TestValue().set({ value: value });
      }

      /*
       * Start an async call and then queue another 10 calls; this keeps the load on the client
       * because it queues it and then batches the result.  The test makes sure that there are
       * only 2 server round trips for all 11 server method calls.
       */
      threadTest.resetSerial();
      var numCalls = manager.getNumberOfCalls();
      var initialComplete = false;
      var simple1 = makeSimple(1);
      threadTest.waitFor(1500, simple1, function(result) { 
        t.debug("1: initial waitFor complete");
        initialComplete = true;
        qx.core.Assert.assertEquals(result, 0);
      });
      setTimeout(function() {
        var numComplete = 0;
        for (var i = 0; i < 10; i++) {
          threadTest.waitFor(10, simple1, function(i, result) {
            t.debug("1: threadTest.waitFor #" + i + " complete");
            numComplete++;
            qx.core.Assert.assertEquals(result, i + 1);
            if (numComplete == 10) {
              qx.core.Assert.assertEquals(manager.getNumberOfCalls(), numCalls + 2);
              qx.core.Assert.assertTrue(initialComplete);
              testForceSync();
            }
          }.bind(t, i));
          qx.core.Assert.assertEquals(manager.getNumberOfCalls(), numCalls + 1);
        }
      }, 100);
      
      /*
       * Use a synchronous method call to force the queue to be flushed 
       */
      function testForceSync() {
        threadTest.resetSerial();
        var numCalls = manager.getNumberOfCalls();
        var initialComplete = false;
        var simple2 = makeSimple(2);
        threadTest.waitFor(1500, simple2, function(result) { 
          t.debug("2: initial waitFor complete, result=" + result);
          
          // This must stil be zero because the server will block other 
          //  requests until this request is complete
          qx.core.Assert.assertEquals(result, 0);
        });
        setTimeout(function() {
          var numComplete = 0;
          for (var i = 0; i < 10; i++) {
            threadTest.waitFor(10, simple2, function(i, result) {
              t.debug("2: threadTest.waitFor #" + i + " complete, result=" + result);
              numComplete++;
              if (numComplete == 10) {
                qx.core.Assert.assertEquals(manager.getNumberOfCalls(), numCalls + 2);
                qx.core.Assert.assertFalse(initialComplete);
                qx.core.Assert.assertEquals(result, i + 1);
              }
            }.bind(t, i));
            qx.core.Assert.assertEquals(manager.getNumberOfCalls(), numCalls + 1);
          }
          
          // Do a synchronous call to force server connection
          var result = threadTest.waitFor(1, simple2);
          t.debug("2: force sync waitFor=" + result);
          qx.core.Assert.assertEquals(result, 11);
          cb();
        }, 100);
      }
    },
    
    testRecursiveArray: function() {
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      
      function create(id, children) {
        var item = new com.zenesis.qx.remote.test.collections.TestRecursiveArray();
        item.setId(id);
        if (children)
          append(item, children);
        return item;
      }
      
      function append(item, children) {
        children.forEach(function(child) {
          item.getChildren().push(child);
        });
        return item;
      }
      var bravo = create("bravo");
      var root = create("root", [
        create("alpha"),
        bravo,
        create("charlie"),
        create("delta")
      ]);
      boot.setRecursiveArray(root);
      append(bravo, [
          create("bravo-alpha"),
          create("bravo-bravo"),
          create("bravo-charlie")
        ]);
      
      var rootChildren = root.getChildren();
      var bravoChildren = bravo.getChildren();
      boot.testRecursiveArray();
      this.assertIdentical(root, boot.getRecursiveArray());
      this.assertEquals(4, root.getChildren().getLength());
      this.assertEquals("alpha", root.getChildren().getItem(0).getId());
      this.assertEquals("bravo", root.getChildren().getItem(1).getId());
      this.assertEquals("charlie", root.getChildren().getItem(2).getId());
      this.assertEquals("delta", root.getChildren().getItem(3).getId());
      
      this.assertIdentical(bravo, root.getChildren().getItem(1));
      this.assertEquals(3, bravo.getChildren().getLength());
      this.assertEquals("bravo-alpha", bravo.getChildren().getItem(0).getId());
      this.assertEquals("bravo-bravo", bravo.getChildren().getItem(1).getId());
      this.assertEquals("bravo-charlie", bravo.getChildren().getItem(2).getId());
    },

    testArrayLists: function() {
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      var tc = boot.getArrayListTests();
      var arr = tc.getStringArray();
      qx.core.Assert.assertArrayEquals([ "alpha", "bravo", "charlie", "delta", "echo" ], arr.toArray());
      arr.removeAt(2);
      arr.removeAt(2);
      arr.push("foxtrot");
      arr.push("george");
      tc.makeChanges();
      qx.core.Assert.assertArrayEquals([ "alpha", "bravo", "echo", "foxtrot", "george", "henry", "indigo" ], arr.toArray());
    },
    
    testMaps: function() {
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      var mc = boot.getMapTests();
      var map = mc.getStringMap();
      qx.core.Assert.assertTrue(map instanceof com.zenesis.qx.remote.Map);
      qx.core.Assert.assertEquals(map.getLength(), 5);
      qx.core.Assert.assertEquals(map.get("alpha"), "one");
      qx.core.Assert.assertEquals(map.get("bravo"), "two");
      qx.core.Assert.assertEquals(map.get("charlie"), "three");
      qx.core.Assert.assertEquals(map.get("delta"), "four");
      qx.core.Assert.assertEquals(map.get("echo"), "five");
      map.remove("bravo");
      map.remove("delta");
      map.put("alpha", "first");
      mc.makeChanges();
      qx.core.Assert.assertEquals(map.getLength(), 5);
      qx.core.Assert.assertFalse(map.containsKey("bravo"));
      qx.core.Assert.assertFalse(map.containsKey("delta"));
      qx.core.Assert.assertEquals(map.get("alpha"), "first again");
      qx.core.Assert.assertEquals(map.get("foxtrot"), "six");
      qx.core.Assert.assertEquals(map.get("george"), "seven");
    },
    
    testObjectKeyMaps: function() {
      var EXPECTED = {
          alpha: "one",
          bravo: "two",
          charlie: "three"
      }
      var VALUES = [];
      for (var name in EXPECTED)
        VALUES.push(EXPECTED[name]);
      
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      var mc = boot.getMapTests();
      var map = mc.getObjectKeyMap();
      qx.core.Assert.assertTrue(map instanceof com.zenesis.qx.remote.Map);
      qx.core.Assert.assertEquals(map.getLength(), 3);
      map.getKeys().forEach(function(key) {
        qx.core.Assert.assertTrue(key instanceof com.zenesis.qx.remote.test.collections.TestQsoMap$MyKey);
        qx.core.Assert.assertTrue(typeof key.getKeyId() == "string");
        qx.core.Assert.assertTrue(!!EXPECTED[key.getKeyId()]);
        var value = map.get(key);
        qx.core.Assert.assertTrue(value instanceof com.zenesis.qx.remote.test.collections.TestQsoMap$MyValue);
        qx.core.Assert.assertEquals(EXPECTED[key.getKeyId()], value.getValueId());
      });
      map.getValues().forEach(function(value) {
        qx.core.Assert.assertTrue(value instanceof com.zenesis.qx.remote.test.collections.TestQsoMap$MyValue);
        qx.core.Assert.assertTrue(typeof value.getValueId() == "string");
        qx.core.Assert.assertTrue(VALUES.indexOf(value.getValueId()) > -1);
      });
    },
    
    testThreading: function() {
      var t = this;
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var boot = manager.getBootstrapObject();
      var threadTest = boot.getThreadTest();
      
      var btn = new qx.ui.form.Button("Test Threading");
      btn.addListener("execute", function(evt) {
        for (var i = 0; i < MAX; i++) {
          stuff(i);
        }
      }, this);
      var root = this.getRoot();
      root.add(btn, { left: 100, top: 300 });
      
      var results = [];
      var count = 0;
      var MAX = 100;
      
      function stuff(index) {
        var arr = new qx.data.Array();
        for (var i = 0; i < 10; i++) {
          var myTp = new com.zenesis.qx.remote.test.properties.TestProperties();
          myTp.setWatchedString("setByClientProperty");
          arr.push(myTp);
        }
        threadTest.tryThis(arr, function(result) {
          complete(index);
        });
      }
      
      function complete(index, result) {
        results[index] = result;
        count++;
        if (count == MAX)
          t.log("Completed Thread test");
      }
      
    },
    
    testMultiUser: function() {
      var t = this;
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var boot = manager.getBootstrapObject();
      var multiUser = boot.getMultiUser();
      var root = this.getRoot();

      var btnThrash = new qx.ui.form.Button("Start Thrash Test");
      root.add(btnThrash, {
        left: 100,
        top: 125
      });
      btnThrash.addListener("execute", function() {
        var count = 0;
        function test() {
          multiUser.thrashTest(count++);
          if ((count % 100) == 0)
            t.log(count + "...");
          if (count < 2000)
            setTimeout(test, parseInt(Math.random()*100));
        }
        test();
      }, this);
      
      var btnReset = new qx.ui.form.Button("Reset Users");
      root.add(btnReset, {
        left: 100,
        top: 50
      });
      btnReset.addListener("execute", function() {
        multiUser.resetAll();
      }, this);
      
      var btnStart = new qx.ui.form.Button("Start Multiuser Test");
      root.add(btnStart, {
        left: 200,
        top: 50
      });
      btnStart.addListener("execute", function() {
        multiUser.startTest();
      }, this);
      
      var btnUpdate = new qx.ui.form.Button("Update Values").set({ enabled: false });
      root.add(btnUpdate, {
        left: 350,
        top: 50
      });
      btnUpdate.addListener("execute", function() {
        makeChanges();
      }, this);
      
      var cbxAutoUpdate = new qx.ui.form.CheckBox("Auto update values").set({ value: true });
      root.add(cbxAutoUpdate, { left: 350, top: 80 });
      
      var lblNumUsers = new qx.ui.basic.Label("0 Users Ready").set({ allowGrowX: true });
      root.add(lblNumUsers, {
        left: 100,
        top: 20
      });
      
      var status;
      var numCalls = 0;
      
      function checkForReady() {
        if (t.__inTimeouts) {
          setTimeout(checkForReady, 500);
          return;
        }
        
        status = multiUser.checkReady();
        //t.log("status=" + JSON.stringify(status));
        lblNumUsers.setValue(status.numReady + " Users Ready");
        if (status.yourIndex == 0)
          setTimeout(checkForReady, 500);
        else {
          btnUpdate.setEnabled(true);
          if (cbxAutoUpdate.getValue()) {
            makeChanges();
          }
        }
      }
      
      function arrayToString(arr) {
        var str = "";
        arr.forEach(function(key) {
          if (str.length)
            str += ", ";
          str += key;
        });
        return "[ " + str + " ]";
      }
      
      function mapToString(map) {
        var str = "";
        map.getKeys().forEach(function(key) {
          if (str)
            str += ",\n";
          str += "  " + key + " = " + map.get(key);
        });
        return "{\n" + str + "\n}";
      }
      
      function makeChanges() {
        //syncUsers();
        numCalls = manager.getNumberOfCalls();
        
        t.log("Starting, yourIndex=" + status.yourIndex);
        var index = status.yourIndex - 1;
        var stringMap = multiUser.getStringMap();
        var stringArray = multiUser.getStringArray();
        t.log("stringArray = " + arrayToString(stringArray));
        t.log("stringMap = " + mapToString(stringMap));
        
        var key = stringArray.getItem(index);
        var value = stringMap.get(key);
        stringMap.put(key, value + " by " + status.yourIndex);
        
        qx.core.Assert.assertEquals(numCalls, manager.getNumberOfCalls());
        multiUser.noop();
        
        t.log("stringArray = " + arrayToString(stringArray));
        t.log("stringMap = " + mapToString(stringMap));
        
        function stepTwo() {
          stringArray.remove(key);
          multiUser.noop();
          t.log("stringArray = " + arrayToString(stringArray));
          t.log("stringMap = " + mapToString(stringMap));
        }
        if (cbxAutoUpdate.getValue()) {
          var waitFor = ((status.numReady - (status.yourIndex - 1)) * 1000) + 250;
          t.log("Waiting for " + waitFor + "ms");
          setTimeout(stepTwo, waitFor);
        } else
          stepTwo();
      }
      
      checkForReady();
      
    },
    
    __inTimeouts: false,
    
    testBrowserTimeouts: function() {
      var t = this;
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var boot = manager.getBootstrapObject();
      var threadTest = boot.getThreadTest();
      var root =this.getRoot();
      
      var btn = new qx.ui.form.Button("Start Long Timeout");
      root.add(btn, {
        left: 100,
        top: 250
      });
      btn.addListener("execute", function() {
        btn.setEnabled(false);
        t.__inTimeouts = true;
        var secs = 5 * 60;
        t.log("Waiting for " + secs + " seconds synchronously ");
        threadTest.waitFor(secs * 1000, null);
        t.log("Waiting for " + secs + " seconds asynchronously ");
        threadTest.waitFor(secs * 1000, null, function(result) {
          btn.setEnabled(true);
          t.__inTimeouts = false;
          t.log("Wait finished");
        });
      }, this);
    }
  }
});
