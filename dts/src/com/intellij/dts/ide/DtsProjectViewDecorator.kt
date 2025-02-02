package com.intellij.dts.ide

import com.intellij.dts.settings.DtsSettings
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import kotlinx.coroutines.CoroutineScope

class DtsProjectViewDecorator(private val project: Project, parentScope: CoroutineScope) : ProjectViewNodeDecorator, DtsSettings.ChangeListener{
    init {
        project.messageBus.connect(parentScope).subscribe(DtsSettings.ChangeListener.TOPIC, this)
    }

    override fun settingsChanged(settings: DtsSettings) {
        ProjectView.getInstance(project).refresh()
    }

    override fun decorate(node: ProjectViewNode<*>?, data: PresentationData?) {
        val settings = DtsSettings.of(project)
        if (settings.zephyrCMakeSync) return

        val board = settings.zephyrBoard ?: return

        val file = node?.virtualFile ?: return
        if (file.nameWithoutExtension != board.name) return

        val icon = data?.getIcon(false) ?: return
        val layeredIcon = LayeredIcon.create(icon, AllIcons.Modules.SourceRootFileLayer)

        data.setIcon(layeredIcon)
    }
}