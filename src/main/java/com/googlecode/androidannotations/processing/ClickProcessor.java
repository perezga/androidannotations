/*
 * Copyright 2010-2011 Pierre-Yves Ricau (py.ricau at gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.androidannotations.processing;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.Id;
import com.googlecode.androidannotations.generation.ClickInstruction;
import com.googlecode.androidannotations.model.Instruction;
import com.googlecode.androidannotations.model.MetaActivity;
import com.googlecode.androidannotations.model.MetaModel;
import com.googlecode.androidannotations.rclass.IRClass;
import com.googlecode.androidannotations.rclass.IRClass.Res;
import com.googlecode.androidannotations.rclass.IRInnerClass;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

public class ClickProcessor implements ElementProcessor {

	private final IRClass rClass;

	public ClickProcessor(IRClass rClass) {
		this.rClass = rClass;
	}

	@Override
	public Class<? extends Annotation> getTarget() {
		return Click.class;
	}

	@Override
	public void process(Element element, MetaModel metaModel) {
		String methodName = element.getSimpleName().toString();

		String clickQualifiedId = extractClickQualifiedId(element);

		Element enclosingElement = element.getEnclosingElement();
		MetaActivity metaActivity = metaModel.getMetaActivities().get(enclosingElement);
		List<Instruction> onCreateInstructions = metaActivity.getOnCreateInstructions();

		ExecutableElement executableElement = (ExecutableElement) element;
		List<? extends VariableElement> parameters = executableElement.getParameters();

		boolean viewParameter = parameters.size() == 1;

		Instruction instruction = new ClickInstruction(methodName, clickQualifiedId, viewParameter);
		onCreateInstructions.add(instruction);

	}

	private String extractClickQualifiedId(Element element) {
		Click annotation = element.getAnnotation(Click.class);
		int idValue = annotation.value();
		IRInnerClass rInnerClass = rClass.get(Res.ID);
		String clickQualifiedId;
		if (idValue == Id.DEFAULT_VALUE) {
			String fieldName = element.getSimpleName().toString();
			int lastIndex = fieldName.lastIndexOf("Clicked");
			if (lastIndex != -1) {
				fieldName = fieldName.substring(0, lastIndex);
			}
			clickQualifiedId = rInnerClass.getIdQualifiedName(fieldName);

		} else {
			clickQualifiedId = rInnerClass.getIdQualifiedName(idValue);
		}
		return clickQualifiedId;
	}

	@Override
	public void process(Element element, JCodeModel codeModel, ActivitiesHolder activitiesHolder) {

		ActivityHolder holder = activitiesHolder.getActivityHolder(element);

		String methodName = element.getSimpleName().toString();

		ExecutableElement executableElement = (ExecutableElement) element;
		List<? extends VariableElement> parameters = executableElement.getParameters();

		boolean hasViewParameter = parameters.size() == 1;

		JFieldRef idRef = extractClickQualifiedId(element, codeModel);

		JDefinedClass onClickListenerClass = codeModel.anonymousClass(codeModel.ref("android.view.View.OnClickListener"));
		JMethod onClickMethod = onClickListenerClass.method(JMod.PUBLIC, codeModel.VOID, "onClick");
		JClass viewClass = codeModel.ref("android.view.View");
		JVar onClickViewParam = onClickMethod.param(viewClass, "view");

		JInvocation clickCall = onClickMethod.body().invoke(methodName);

		if (hasViewParameter) {
			clickCall.arg(onClickViewParam);
		}

		JBlock body = holder.afterSetContentView.body();

		body.add(JExpr.invoke(JExpr.invoke("findViewById").arg(idRef),"setOnClickListener").arg(JExpr._new(onClickListenerClass)));


	}

	private JFieldRef extractClickQualifiedId(Element element, JCodeModel codeModel) {
		Click annotation = element.getAnnotation(Click.class);
		int idValue = annotation.value();
		IRInnerClass rInnerClass = rClass.get(Res.ID);
		if (idValue == Id.DEFAULT_VALUE) {
			String fieldName = element.getSimpleName().toString();
			int lastIndex = fieldName.lastIndexOf("Clicked");
			if (lastIndex != -1) {
				fieldName = fieldName.substring(0, lastIndex);
			}
			return rInnerClass.getIdStaticRef(fieldName, codeModel);

		} else {
			return rInnerClass.getIdStaticRef(idValue, codeModel);
		}
	}

}
