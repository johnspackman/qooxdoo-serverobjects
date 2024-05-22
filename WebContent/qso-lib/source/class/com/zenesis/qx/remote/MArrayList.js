qx.Mixin.define("com.zenesis.qx.remote.MArrayList", {
  construct() {
    this.addListener("change", this.__onArrayListChange, this);
  },

  members: {
    __onArrayListChange(evt) {
      this.detectNulls();
      this.detectDuplicates();
    },

    /**
     * Checks for duplicates.
     * If a duplicate is found and property detectDuplicates is set to true, `debugger;` is called..
     * @returns
     */
    detectDuplicates() {
      if (!this.getDetectDuplicates()) {
        return true;
      }

      let arr = this.toArray();
      for (let i = 0; i < arr.length; i++) {
        let obj = arr[i];
        for (let j = i + 1; j < arr.length; j++) {
          if (arr[j] == obj) {
            debugger;
            console.error("Detected duplicate in arraylist");
          }
        }
      }
    },

    /**
     * Checks for null values.
     * If a null is found and property detectNulls is set to true, `debugger;` is called..
     * @returns
     */
    detectNulls() {
      if (!this.getDetectNulls()) {
        return true;
      }

      let arr = this.toArray();
      for (let i = 0; i < arr.length; i++) {
        if (arr[i] === null) {
          throw new Error("Detected null in arraylist");
        }
      }
    }
  }
});
