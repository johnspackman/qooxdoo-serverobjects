qx.Class.define("com.zenesis.qx.remote.test.TestMap", {
  extend: qx.dev.unit.TestCase,

  members: {
    testSimple() {
      var map = new com.zenesis.qx.remote.Map();
      map.put("one", "ONE");
      map.put("two", "TWO");
      map.put("three", "THREE");
      this.assertEquals("ONE", map.get("one"));

      var entry = map.getEntry("one");
      this.assertNotNull(entry);
      this.assertEquals("one", entry.getKey());
      this.assertEquals("ONE", entry.getValue());

      var triggeredEntryChangeValue = false;
      entry.addListenerOnce("changeValue", evt => {
        this.assertEquals(evt.getData(), "ONE-B");
        triggeredEntryChangeValue = true;
      });

      var triggeredListPut = false;
      map.addListenerOnce("change", evt => {
        var data = evt.getData();
        if (data.type == "put") {
          this.assertEquals(1, data.values.length);
          var value = data.values[0];
          this.assertEquals("one", value.key);
          this.assertEquals("ONE-B", value.value);
          this.assertEquals("ONE", value.oldValue);
          this.assertTrue(value.entry === entry);
          triggeredListPut = true;
        }
      });

      entry.setValue("ONE-B");
      this.assertTrue(triggeredEntryChangeValue);
      this.assertTrue(triggeredListPut);

      this.assertTrue(map.containsValue("ONE-B"));
      this.assertFalse(map.containsValue("ONE"));
      this.assertEquals("ONE-B", map.get("one"));

      map.remove(entry);
      this.assertFalse(map.containsValue("ONE-B"));
      this.assertFalse(map.containsValue("ONE"));
      this.assertFalse(map.containsKey("one"));
      map.getEntries().forEach(
        function (entry) {
          this.assertNotEquals("one", entry.getKey());
          this.assertNotEquals("ONE-B", entry.getValue());
        }.bind(this)
      );
      this.assertEquals(2, map.getEntries().getLength());
      this.assertEquals(2, map.getKeys().getLength());
      this.assertEquals(2, map.getValues().getLength());

      var obj = map.toObject();
      this.assertEquals(2, Object.keys(obj).length);
      for (var name in obj) {
        this.assertTrue(["two", "three"].indexOf(name) > -1);
        this.assertIdentical(map.get(name), obj[name]);
      }
    },

    testObjectKeys() {
      var DT_ONE = new Date();
      var DT_TWO = new Date(DT_ONE.getTime() + 60 * 60 * 1000);
      var DT_THREE = new Date(DT_TWO.getTime() + 60 * 60 * 1000);

      var map = new com.zenesis.qx.remote.Map(true);
      map.put(DT_ONE, "ONE");
      map.put(DT_TWO, "TWO");
      map.put(DT_THREE, "THREE");
      this.assertEquals("ONE", map.get(DT_ONE));

      var entry = map.getEntry(DT_ONE);
      this.assertNotNull(entry);
      this.assertEquals(DT_ONE, entry.getKey());
      this.assertEquals("ONE", entry.getValue());

      var triggeredEntryChangeValue = false;
      entry.addListenerOnce("changeValue", evt => {
        this.assertEquals(evt.getData(), "ONE-B");
        triggeredEntryChangeValue = true;
      });

      var triggeredListPut = false;
      map.addListenerOnce("change", evt => {
        var data = evt.getData();
        if (data.type == "put") {
          this.assertEquals(1, data.values.length);
          var value = data.values[0];
          this.assertEquals(DT_ONE, value.key);
          this.assertEquals("ONE-B", value.value);
          this.assertEquals("ONE", value.oldValue);
          this.assertTrue(value.entry === entry);
          triggeredListPut = true;
        }
      });

      entry.setValue("ONE-B");
      this.assertTrue(triggeredEntryChangeValue);
      this.assertTrue(triggeredListPut);

      this.assertTrue(map.containsValue("ONE-B"));
      this.assertFalse(map.containsValue("ONE"));
      this.assertEquals("ONE-B", map.get(DT_ONE));

      map.remove(entry);
      this.assertFalse(map.containsValue("ONE-B"));
      this.assertFalse(map.containsValue("ONE"));
      this.assertFalse(map.containsKey(DT_ONE));
      map.getEntries().forEach(
        function (entry) {
          this.assertNotEquals("one", entry.getKey());
          this.assertNotEquals("ONE-B", entry.getValue());
        }.bind(this)
      );
      this.assertEquals(2, map.getEntries().getLength());
      this.assertEquals(2, map.getKeys().getLength());
      this.assertEquals(2, map.getValues().getLength());

      var arr = map.toArray();
      this.assertEquals(2, arr.length);
      arr.forEach(
        function (entry) {
          this.assertTrue([DT_TWO, DT_THREE].indexOf(entry.key) > -1);
          this.assertIdentical(map.get(entry.key), entry.value);
        }.bind(this)
      );
    }
  }
});
