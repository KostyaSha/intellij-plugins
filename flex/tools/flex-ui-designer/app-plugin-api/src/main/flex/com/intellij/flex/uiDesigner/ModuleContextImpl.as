package com.intellij.flex.uiDesigner {
import com.intellij.flex.uiDesigner.css.StyleManagerEx;
import com.intellij.flex.uiDesigner.libraries.LibrarySet;

import flash.system.ApplicationDomain;

public final class ModuleContextImpl implements ModuleContextEx {
  private var documentFactories:Vector.<Object>/* FlexDocumentFactory */;
  
  public function ModuleContextImpl(librarySets:Vector.<LibrarySet>, project:Project) {
    _librarySets = librarySets;
    _project = project;
  }

  public function get documentFactoryManager():DocumentFactoryManager {
    return DocumentFactoryManager(project.getComponent(DocumentFactoryManager));
  }

  private var _librariesResolved:Boolean;
  public function get librariesResolved():Boolean {
    return _librariesResolved;
  }

  public function set librariesResolved(value:Boolean):void {
    _librariesResolved = value;
  }

  public function getDocumentFactory(id:int):Object {
    return documentFactories != null && documentFactories.length > id ? documentFactories[id] : null;
  }

  public function putDocumentFactory(id:int, documentFactory:Object):void {
    var requiredLength:int = id + 1;
    if (documentFactories == null) {
      documentFactories = new Vector.<Object>(requiredLength);
    }
    else if (documentFactories.length < requiredLength) {
      documentFactories.length = requiredLength;
    }

    documentFactories[id] = documentFactory;
  }
  
  private var _project:Project;
  public function get project():Project {
    return _project;
  }

  private var _styleManager:StyleManagerEx;
  public function get styleManager():StyleManagerEx {
    return _styleManager;
  }

  public function set styleManager(value:StyleManagerEx):void {
    _styleManager = value;
  }

  private var _librarySets:Vector.<LibrarySet>;
  public function get librarySets():Vector.<LibrarySet> {
    return _librarySets;
  }

  public function get applicationDomain():ApplicationDomain {
    return _librarySets[_librarySets.length - 1].applicationDomain;
  }
  
  private var _inlineCssStyleDeclarationClass:Class;
  public function get inlineCssStyleDeclarationClass():Class {
    if (_inlineCssStyleDeclarationClass == null) {
      _inlineCssStyleDeclarationClass = getClass("com.intellij.flex.uiDesigner.css.InlineCssStyleDeclaration");
    }
    
    return _inlineCssStyleDeclarationClass;
  }
  
  private var _documentFactoryClass:Class;
  public function get documentFactoryClass():Class {
    if (_documentFactoryClass == null) {
      _documentFactoryClass = getClass("com.intellij.flex.uiDesigner.flex.FlexDocumentFactory");
    }

    return _documentFactoryClass;
  }
  
  private var _deferredInstanceFromBytesClass:Class;
  public function get deferredInstanceFromBytesClass():Class {
    if (_deferredInstanceFromBytesClass == null) {
      _deferredInstanceFromBytesClass = getClass("com.intellij.flex.uiDesigner.flex.states.DeferredInstanceFromBytes");
    }

    return _deferredInstanceFromBytesClass;
  }
  
  private var _deferredInstanceFromBytesVectorClass:Class;
  public function get deferredInstanceFromBytesVectorClass():Class {
    if (_deferredInstanceFromBytesVectorClass == null) {
      _deferredInstanceFromBytesVectorClass = getClass("__AS3__.vec::Vector.<com.intellij.flex.uiDesigner.flex.states.DeferredInstanceFromBytes>");
    }

    return _deferredInstanceFromBytesVectorClass;
  }

  private var _effectManagerClass:Class;
  public function get effectManagerClass():Class {
    if (_effectManagerClass == null) {
      _effectManagerClass = getClass("mx.effects.EffectManager");
    }

    return _effectManagerClass;
  }

  public function getClass(fqn:String):Class {
    return Class(applicationDomain.getDefinition(fqn));
  }

  public function getDefinition(fqn:String):Object {
    return applicationDomain.getDefinition(fqn);
  }
}
}
