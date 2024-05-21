qx.Mixin.define("com.zenesis.qx.remote.MArrayList", {
  construct() {
    this.addListener("change", this.__onArrayListChange, this);
  },

  members: {
    __onArrayListChange(evt) {
      this.__detectNulls();
      this.__detectDuplicates();
    },

    /**
     * Checks for duplicates.
     * If a duplicate is found and property detectDuplicates is set to true, `debugger;` is called..
     * @returns
     */
    __detectDuplicates() {
      if (!this.getDetectDuplicates()) return true;

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
    },

    /**
     * Checks for null values.
     * If a null is found and property detectNulls is set to true, `debugger;` is called..
     * @returns
     */
    __detectNulls() {
      if (!this.getDetectNulls()) return true;

      let arr = this.toArray();
      for (let i = 0; i < arr.length; i++) {
        if (arr[i] === null) {
          console.error("Detected null in arraylist");
          debugger;
        }
      }
    }
  }
});
