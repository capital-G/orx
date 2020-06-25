package org.openrndr.extra.dnk3.query

import org.openrndr.extra.dnk3.Material
import org.openrndr.extra.dnk3.Scene
import org.openrndr.extra.dnk3.SceneNode

fun Scene.findNodeByName(name: String): SceneNode? {
    return root.findNodeByName(name)
}

fun SceneNode.findNodeByName(name: String): SceneNode? {

    if (this.name == name) {
        return this
    } else {
        for (child in children) {
            val candidate = child.findNodeByName(name)
            if (candidate != null) {
                return candidate
            }
        }
    }
    return null
}

fun SceneNode.findMaterialByName(name: String): Material? {
    for (entity in entities) {
        if (entity is Material && entity.name == name) {
            return entity
        }
    }

    for (child in children) {
        val candidate = child.findMaterialByName(name)
        if (candidate != null) {
            return candidate
        }
    }
    return null
}