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

qx.Class.define("demoapp.test.DemoTest", {
	extend : qx.dev.unit.TestCase,

	members : {
		testMap : function() {
			var map = new com.zenesis.qx.remote.Map();
			map.put("bravo", "two");
			map.put("charlie", "three");
			this.assertEquals(2, map.getLength());
			this.assertEquals("two", map.get("bravo"));
			this.assertUndefined(map.get("zulu"));
			this.assertTrue(map.getKeys().contains("bravo"));
			this.assertTrue(map.getValues().contains("two"));
			map.put("bravo", "second");
			this.assertTrue(map.getKeys().contains("bravo"));
			this.assertFalse(map.getValues().contains("two"));
			this.assertTrue(map.getValues().contains("second"));
			
			var self = this;
			this.assertEventFired(map, "change", function() {
				map.put("alpha", "one");
			}, function(evt) {
				var data = evt.getData();
				self.assertEquals("put", data.type);
				self.assertEquals(1, data.values.length);
				self.assertEquals("alpha", data.values[0].key);
				self.assertEquals("one", data.values[0].value);
			});
			
			this.assertEventFired(map, "change", function() {
				map.remove("bravo");
			}, function(evt) {
				var data = evt.getData();
				self.assertEquals("remove", data.type);
				self.assertEquals(1, data.values.length);
				self.assertEquals("bravo", data.values[0].key);
				self.assertEquals("second", data.values[0].value);
			});
			
			var count = 0;
			map.addListener("change", function(evt) {
				count++;
				this.assertTrue(count < 3);
				var data = evt.getData();
				if (data.type == "put") {
					this.assertEquivalent([
						{ key: "charlie", value: "third", oldValue: "three", entry: undefined },
						{ key: "delta", value: "four", entry: undefined },
						{ key: "echo", value: "five", entry: undefined }
					], data.values);
				} else if (data.type == "remove") {
					this.assertEquivalent([
					    { key: "alpha", value: "one", entry: undefined }
					], data.values);
				}
			}, this);
			map.replace({
				"charlie": "third",
				"delta": "four",
				"echo": "five"
			});
			this.assertEquals(2, count);
		},
		
		testObserver: function() {
		  var t = this;
		  var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
		  var to = boot.getTestObserver();
		  function doTest(name) {
	      var obj = to["create" + qx.lang.String.firstUp(name)]();
	      obj.setSomeValue("hello "+ name)
	      obj.doStuff();
	      t.assertEquals(true, obj.isDirty());
		  }
      doTest("staticInner1");
      doTest("staticInner2");
      doTest("inner");
      
      var obj = to.createInner();
      t.assertEquals(false, obj.isDirty());
      obj.getSimpleArray().push("alpha");
      obj.doStuff();
      t.assertEquals(true, obj.isDirty());
      
      var obj = to.createInner();
      t.assertEquals(false, obj.isDirty());
      obj.getQsoArrayList().push("zulu");
      obj.doStuff();
      t.assertEquals(true, obj.isDirty());
		},
		
		testDate() {
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      var tp = boot.getMainTests().getTestProperties();
      const addUp = dt => dt.getHours() + dt.getMinutes() + dt.getSeconds();
      
      this.assertTrue(addUp(tp.getDateTime()) != 0);
      this.assertTrue(addUp(tp.getDateStartOfDay()) == 0);
      this.assertTrue(addUp(tp.getDateEndOfDay()) == 23 + 59 + 59);
      
      let dt1 = new Date();
      tp.setDateStartOfDay(dt1);
      tp.doStuff();
      let dt2 = tp.getDateStartOfDay();
      this.assertTrue(addUp(dt2) == 0);
      this.assertEquals(dt2.getFullYear(), dt1.getFullYear());
      this.assertEquals(dt2.getMonth(), dt1.getMonth());
      this.assertEquals(dt2.getDate(), dt1.getDate());
      tp.checkDateStartOfDay();
		},
		
		assertEquivalent: function(expected, actual, msg) {
			if (qx.lang.Type.isArray(expected)) {
				this.assertTrue(qx.lang.Type.isArray(actual), msg);
				this.assertEquals(expected.length, actual.length, msg);
				for (var i = 0; i < expected.length; i++) {
					this.assertEquivalent(expected[i], actual[i], msg);
				}
			} else if (qx.lang.Type.isObject(expected)) {
				this.assertTrue(qx.lang.Type.isObject(actual), msg);
				this.assertEquals(Object.keys(expected).length, Object.keys(actual).length, msg);
				for (var name in Object.keys(expected)) {
				  if (expected[name] !== undefined)
				    this.assertEquivalent(expected[name], actual[name], msg);
				}
			} else
				this.assertEquals(expected, actual, msg);
		}
	}
});
