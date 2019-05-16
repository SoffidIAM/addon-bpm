//
// (C) 2013 Soffid
//
//

package com.soffid.iam.addons.bpm.common;
/**
 * ValueObject Transition
 **/
public class Transition

		implements java.io.Serializable
 {

	/**
	 + The serial version UID of this class. Needed for serialization.
	 */
	private static final long serialVersionUID = 1;
	/**
	 * Attribute id

	 */
	private java.lang.Long id;

	/**
	 * Attribute source

	 */
	private com.soffid.iam.addons.bpm.common.Node source;

	/**
	 * Attribute target

	 */
	private com.soffid.iam.addons.bpm.common.Node target;

	/**
	 * Attribute name

	 */
	private java.lang.String name;

	/**
	 * Attribute script

	 */
	private java.lang.String script;

	public Transition()
	{
	}

	public Transition(java.lang.Long id, com.soffid.iam.addons.bpm.common.Node source, com.soffid.iam.addons.bpm.common.Node target, java.lang.String name, java.lang.String script)
	{
		super();
		this.id = id;
		this.source = source;
		this.target = target;
		this.name = name;
		this.script = script;
	}

	public Transition(Transition otherBean)
	{
		this(otherBean.id, otherBean.source, otherBean.target, otherBean.name, otherBean.script);
	}

	/**
	 * Gets value for attribute id
	 */
	public java.lang.Long getId() {
		return this.id;
	}

	/**
	 * Sets value for attribute id
	 */
	public void setId(java.lang.Long id) {
		this.id = id;
	}

	/**
	 * Gets value for attribute source
	 */
	public com.soffid.iam.addons.bpm.common.Node getSource() {
		return this.source;
	}

	/**
	 * Sets value for attribute source
	 */
	public void setSource(com.soffid.iam.addons.bpm.common.Node source) {
		this.source = source;
	}

	/**
	 * Gets value for attribute target
	 */
	public com.soffid.iam.addons.bpm.common.Node getTarget() {
		return this.target;
	}

	/**
	 * Sets value for attribute target
	 */
	public void setTarget(com.soffid.iam.addons.bpm.common.Node target) {
		this.target = target;
	}

	/**
	 * Gets value for attribute name
	 */
	public java.lang.String getName() {
		return this.name;
	}

	/**
	 * Sets value for attribute name
	 */
	public void setName(java.lang.String name) {
		this.name = name;
	}

	/**
	 * Gets value for attribute script
	 */
	public java.lang.String getScript() {
		return this.script;
	}

	/**
	 * Sets value for attribute script
	 */
	public void setScript(java.lang.String script) {
		this.script = script;
	}

	/**
	 * Returns a string representation of the value object.
	 */
	public String toString()
	{
		StringBuffer b = new StringBuffer();
		b.append (getClass().getName());
		b.append ("[id: ");
		b.append (this.id);
		b.append (", source: ");
		b.append (this.source == null ? null: this.source.getName());
		b.append (", target: ");
		b.append (this.target == null ? null: this.target.getName());
		b.append (", name: ");
		b.append (this.name);
		b.append (", script: ");
		b.append (this.script);
		b.append ("]");
		return b.toString();
	}

}
