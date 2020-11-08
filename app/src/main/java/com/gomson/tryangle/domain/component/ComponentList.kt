package com.gomson.tryangle.domain.component

class ComponentList: ArrayList<Component>() {

    var maxId: Long = 1

    private fun getNewComponentId(): Long {
        maxId++
        return maxId - 1
    }

    override fun add(element: Component): Boolean {
        element.componentId = getNewComponentId()
        return super.add(element)
    }

    override fun add(index: Int, element: Component) {
        element.componentId = getNewComponentId()
        super.add(index, element)
    }

    override fun addAll(elements: Collection<Component>): Boolean {
        for (component in elements) {
            component.componentId = getNewComponentId()
        }
        return super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<Component>): Boolean {
        for (component in elements) {
            component.componentId = getNewComponentId()
        }
        return super.addAll(index, elements)
    }

    fun getObjectComponentList(): ArrayList<ObjectComponent> {
        val objectComponentList = ArrayList<ObjectComponent>()
        for (component in this) {
            if (component is ObjectComponent) {
                objectComponentList.add(component)
            }
        }
        return objectComponentList
    }
}