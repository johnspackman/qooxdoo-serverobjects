qx.Mixin.define("com.zenesis.qx.remote.MArrayList", {
  construct() {
    this.addListener("change", this.__onArrayListChange, this);
  },

  members: {
    __onArrayListChange(evt) {
      let arr = this.toArray();
      for (let i = 0; i < arr.length; i++) {
        let obj = arr[i];
        for (let j = i + 1; j < arr.length; j++) {
          if (arr[j] == obj) {
            console.error("Detected duplicate in arraylist");
            debugger;
          }
        }
      }
    }
  }
});
