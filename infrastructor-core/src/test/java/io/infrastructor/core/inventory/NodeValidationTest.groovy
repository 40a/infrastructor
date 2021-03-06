package io.infrastructor.core.inventory

import io.infrastructor.core.validation.ValidationException
import org.junit.Test

import static io.infrastructor.core.inventory.InlineInventory.inlineInventory

class NodeValidationTest {
    
    @Test(expected = ValidationException)
    void nodeMustHaveAHost() {
        inlineInventory {
            node(port: 10000, username: "root")
        }.provision()  
    }
    
    @Test(expected = ValidationException)
    void portMustNotBeNull() {
        inlineInventory {
            node(host: "host", port: null, username: "root")
        }.provision()    
    }
    
    @Test(expected = ValidationException)
    void usernameMustNotBeNull() {
        def inventory = inlineInventory {
            node(host: "host", port: 10000, username: null)
        }.provision()   
    }
}
