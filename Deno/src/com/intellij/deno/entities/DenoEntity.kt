package com.intellij.deno.entities

import com.intellij.workspaceModel.storage.*
import com.intellij.workspaceModel.storage.url.VirtualFileUrl
import org.jetbrains.deft.ObjBuilder
import org.jetbrains.deft.Type

internal object DenoEntitySource : EntitySource

interface DenoEntity : WorkspaceEntity {
  val depsFile: VirtualFileUrl?
  val denoTypes: VirtualFileUrl?

  //region generated code
  @GeneratedCodeApiVersion(1)
  interface Builder : DenoEntity, WorkspaceEntity.Builder<DenoEntity>, ObjBuilder<DenoEntity> {
    override var entitySource: EntitySource
    override var depsFile: VirtualFileUrl?
    override var denoTypes: VirtualFileUrl?
  }

  companion object : Type<DenoEntity, Builder>() {
    operator fun invoke(entitySource: EntitySource, init: (Builder.() -> Unit)? = null): DenoEntity {
      val builder = builder()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyEntity(entity: DenoEntity, modification: DenoEntity.Builder.() -> Unit) = modifyEntity(
  DenoEntity.Builder::class.java, entity, modification)
//endregion
