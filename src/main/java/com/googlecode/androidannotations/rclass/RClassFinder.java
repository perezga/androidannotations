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
package com.googlecode.androidannotations.rclass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.googlecode.androidannotations.annotations.Enhance;
import com.googlecode.androidannotations.helper.AnnotationHelper;
import com.googlecode.androidannotations.model.AnnotationElements;

public class RClassFinder extends AnnotationHelper {

    public RClassFinder(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    public IRClass findFromAnnotationElements(AnnotationElements extractedModel) {

        Elements elementUtils = processingEnv.getElementUtils();

        Set<? extends Element> annotatedElements = extractedModel.getAnnotatedElements(Enhance.class);

        Iterator<? extends Element> iterator = annotatedElements.iterator();

        if (iterator.hasNext()) {
            TypeElement firstLayoutAnnotatedElement = (TypeElement) iterator.next();

            // TODO better handling at finding R class
            PackageElement firstActivityPackage = elementUtils.getPackageOf(firstLayoutAnnotatedElement);

            TypeElement rType = elementUtils.getTypeElement(firstActivityPackage.getQualifiedName() + "." + "R");

            if (rType != null) {
                return new RClass(rType);
            } else {
                printAnnotationError(firstLayoutAnnotatedElement, Enhance.class, "In order to find the R class, all Activities annotated with @" + Enhance.class.getSimpleName() + " should belong to the same package as the R class, which is not the case for: " + firstLayoutAnnotatedElement);
                return IRClass.EMPTY_R_CLASS;
            }
        } else {
            return IRClass.EMPTY_R_CLASS;
        }
    }

    public IRClass findFromFilesystem() throws IOException {
        Filer filer = processingEnv.getFiler();

        FileObject res = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "");
        File projectRoot = new File(res.toUri()).getParentFile();

        File resFolder = new File(projectRoot, "res");
        
        File androidManifestFile = new File(projectRoot, "AndroidManifest.xml");
        
        FileReader reader = new FileReader(androidManifestFile);

        BufferedReader br = new BufferedReader(reader);

        String line;
        String rClassPackage = "";
        while ((line = br.readLine()) != null) {
            int idIndex = line.indexOf("package=\"");
            if (idIndex != -1) {
                int endOfId = line.lastIndexOf("\"");
                rClassPackage = line.substring(idIndex + "package=\"".length() + 1, endOfId);
                break;
            }
        }
        
        // return new FileSystemRClass(resFolder, rClassPackage);
        return null;
    }

}
