/**
 * ************************************************************************
 *
 *    server-objects - a contrib to the Qooxdoo project that makes server
 *    and client objects operate seamlessly; like Qooxdoo, server objects
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 *
 *    http://qooxdoo.org
 *
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 *
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory
 *      for details.
 *
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 *
 * ************************************************************************
 */
package com.zenesis.qx.remote;

public class CommandId {

  public enum CommandType {
    BOOTSTRAP("bootstrap"), FIRE_EVENT("fire"), FUNCTION_RETURN("return"), EXCEPTION("exception"),
    MAP_CLIENT_ID("mapClientId"), RESTORE_VALUE("restore"), SET_VALUE("set"), EXPIRE("expire"), DEFINE("define"),
    EDIT_ARRAY("edit-array", true), LOAD_TYPE("load-type"), UPLOAD("upload"), PUBLISH("publish");

    public final String remoteId;
    public final boolean cumulative;

    private CommandType(String remoteId) {
      this.remoteId = remoteId;
      this.cumulative = false;
    }

    private CommandType(String remoteId, boolean cumulative) {
      this.remoteId = remoteId;
      this.cumulative = cumulative;
    }
  }

  public final CommandType type;
  public final Object object;
  public final String name;

  /**
   * @param object
   * @param propertyName
   */
  public CommandId(CommandType type, Object object, String propertyName) {
    super();
    this.type = type;
    this.object = object;
    this.name = propertyName;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    CommandId that = (CommandId) obj;
    return that.type == type && that.object == object &&
        ((name == null && that.name == null) || (name != null && that.name != null && that.name.equals(name)));
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int hash = type.hashCode();
    if (object != null)
      hash ^= object.hashCode();
    if (name != null)
      hash ^= name.hashCode();
    return hash;
  }
}
