qx.Class.define("com.zenesis.qx.remote.annotations.Property", {
  extend: qx.core.Object,

  properties: {
    componentTypeName: {
      check: "String"
    },

    keyTypeName: {
      check: "String"
    },

    readOnly: {
      init: false,
      check: "Boolean"
    }
  }
});
