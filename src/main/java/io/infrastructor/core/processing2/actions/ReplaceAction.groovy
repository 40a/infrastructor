package io.infrastructor.core.processing2.actions

import io.infrastructor.core.inventory.Node
import javax.validation.constraints.NotNull

public class ReplaceAction extends AbstractNodeAction {
    
    @NotNull
    def target
    @NotNull
    def regexp
    @NotNull
    def content
    def owner
    def group
    def mode
    def all = false
    def sudo = false
    
    def execute() {
        def stream = new ByteArrayOutputStream()
        node.readFile(target, stream, sudo)
            
        def original = stream.toString()
        def updated = all ? original.replaceAll(regexp, content) : original.replaceFirst(regexp, content)

        node.writeText(target, updated, sudo)
        node.updateOwner(target, owner, sudo)
        node.updateGroup(target, group, sudo)
        node.updateMode(target, mode, sudo)
    }
}

