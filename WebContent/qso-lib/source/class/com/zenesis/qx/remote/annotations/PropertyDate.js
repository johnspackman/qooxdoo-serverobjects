qx.Class.define("com.zenesis.qx.remote.annotations.PropertyDate", {
  extend: qx.core.Object,
  
  properties: {
    value: {
      check: [ "date", "dateTime" ]
    },
    
    zeroTime: {
      check: "Boolean"
    }
  }
});
