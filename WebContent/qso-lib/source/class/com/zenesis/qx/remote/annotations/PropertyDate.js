qx.Class.define("com.zenesis.qx.remote.annotations.PropertyDate", {
  extend: qx.core.Object,

  properties: {
    value: {
      init: "dateTime",
      nullable: false,
      check: ["date", "dateTime"]
    },

    zeroTime: {
      init: false,
      nullable: false,
      check: "Boolean"
    }
  }
});
