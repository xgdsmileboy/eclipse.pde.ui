/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;


/**
 * Describes a contributed javadoc tag
 * 
 * @noimplement this interface is not to be implemented by clients
 * 
 * @since 1.0.0
 */
public interface IApiJavadocTag {

	/**
	 * Type constant representing the tag applies to a Java class.
	 * Values is <code>1</code>
	 */
	public static final int TYPE_CLASS = 0x1;
	/**
	 * Type constant representing the tag applies to a Java interface.
	 * Values is <code>2</code>
	 */
	public static final int TYPE_INTERFACE = 0x1 << 1;
	/**
	 * Member constant representing the tag applies to a Java method.
	 * Values is <code>4</code>
	 */
	public static final int MEMBER_METHOD = 0x1 << 2; 
	/**
	 * Member constant representing the tag applies to a Java field.
	 * Values is <code>8</code>
	 */
	public static final int MEMBER_FIELD = 0x1 << 3;
	
	/**
	 * Member constant representing the tag applies to a Java type declaration
	 * - not to a member.
	 * Values is <code>16</code>
	 */
	public static final int MEMBER_NONE = 0x1 << 4;
	
	/**
	 * Returns the restriction modifier for the tag, or 
	 * {@link RestrictionModifiers#NO_RESTRICTIONS} if the restriction 
	 * cannot be parsed into an integer. 
	 * See {@link RestrictionModifiers} for a listing of platform
	 * modifiers.
	 * 
	 * @return the restriction modifier for the tag or {@link RestrictionModifiers#NO_RESTRICTIONS}
	 */
	public int getRestrictionModifier();
	
	/**
	 * Returns the formatted javadoc tag label. A formatted javadoc tag label takes the 
	 * form '@'+'tag name'.
	 * <br>
	 * For example:
	 * <pre>
	 * <code>@noimplement</code>
	 * </pre>
	 * @return the formatted javadoc tag label
	 */
	public String getTagLabel();
	
	/**
	 * Returns the complete formatted javadoc tag for an element of the specified
	 * kind. A complete javadoc tag takes the form '@'+'tagname'+'tagcomment'
	 *  <br>
	 * For example:
	 * <pre>
	 * <code>@noimplement</code> this interface is not to be implemented by clients
	 * </pre>
	 * 
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or <code>NONE</code>
	 * @return the complete javadoc tag
	 */
	public String getCompleteTag(int type, int member);
	
	/**
	 * Returns whether this tag is applicable to the specified kind of type and member.
	 * 
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or <code>NONE</code>
	 * @return whether this tag applies to this kind of element
	 */
	public boolean isApplicable(int type, int member);
	
}
