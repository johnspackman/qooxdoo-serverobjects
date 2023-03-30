/**
 * Holds an entry in a Map; this object is purely used to store the key/value pairing, and is allowed
 * to change the value property but MUST NOT change the key property.
 */
qx.Class.define("com.zenesis.qx.remote.Entry", {
  extend: qx.core.Object,

  construct(key, value) {
    super();
    this.set({ key: key, value: value });
  },

  properties: {
    key: {
      nullable: false
    },

    value: {
      init: null,
      nullable: true,
      event: "changeValue"
    }
  }
});
