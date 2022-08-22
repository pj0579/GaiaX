/*
 * Copyright (c) 2021, Alibaba Group Holding Limited;
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.gaiax.render

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.gaiax.GXTemplateEngine
import com.alibaba.gaiax.context.GXTemplateContext
import com.alibaba.gaiax.render.node.GXNode
import com.alibaba.gaiax.render.node.GXNodeTreeCreator
import com.alibaba.gaiax.render.node.GXNodeTreeUpdater
import com.alibaba.gaiax.render.utils.GXContainerUtils
import com.alibaba.gaiax.render.utils.GXIManualExposureEventListener
import com.alibaba.gaiax.render.view.GXIRootView
import com.alibaba.gaiax.render.view.GXViewTreeCreator
import com.alibaba.gaiax.render.view.GXViewTreeUpdater

/**
 * @suppress
 */
class GXRenderImpl {

    fun createNode(gxTemplateContext: GXTemplateContext): GXNode {
        val rootNode = GXNodeTreeCreator.create(gxTemplateContext)
        gxTemplateContext.rootNode = rootNode
        return rootNode
    }

    fun bindNodeData(gxTemplateContext: GXTemplateContext) {
        gxTemplateContext.isDirty = false

        // Update the virtual node tree
        GXNodeTreeUpdater(gxTemplateContext).buildLayoutAndStyle()
    }

    fun createView(gxTemplateContext: GXTemplateContext): View {
        // Create a virtual node tree
        val rootNode = GXNodeTreeCreator.create(gxTemplateContext)
        gxTemplateContext.rootNode = rootNode

        // Create a view based on the virtual node tree
        val rootView = GXViewTreeCreator(gxTemplateContext, rootNode).build().apply {
            (this as GXIRootView).setTemplateContext(gxTemplateContext)
        }
        gxTemplateContext.rootView = rootView

        return gxTemplateContext.rootView
            ?: throw IllegalArgumentException("Create template view exception, gxTemplateContext = $gxTemplateContext")
    }

    fun createViewOnlyNodeTree(gxTemplateContext: GXTemplateContext): GXNode {
        // Create a virtual node tree
        val rootNode = GXNodeTreeCreator.create(gxTemplateContext)
        gxTemplateContext.rootNode = rootNode
        return rootNode
    }

    fun createViewOnlyViewTree(gxTemplateContext: GXTemplateContext): View {
        val rootNode = gxTemplateContext.rootNode
            ?: throw IllegalArgumentException("Create template view exception, root node null, $gxTemplateContext")

        // Create a view based on the virtual node tree
        val rootView = GXViewTreeCreator(gxTemplateContext, rootNode).build().apply {
            (this as GXIRootView).setTemplateContext(gxTemplateContext)
        }
        gxTemplateContext.rootView = rootView

        return gxTemplateContext.rootView
            ?: throw IllegalArgumentException("Create template view exception, gxTemplateContext = $gxTemplateContext")
    }

    fun bindViewDataOnlyNodeTree(gxTemplateContext: GXTemplateContext) {
        processContainerItemManualExposureWhenScrollStateChanged(gxTemplateContext)

        // Resetting the Template Status
        gxTemplateContext.isDirty = false

        // Update the node tree
        GXNodeTreeUpdater(gxTemplateContext).buildNodeLayout()
    }

    private fun processContainerItemManualExposureWhenScrollStateChanged(gxTemplateContext: GXTemplateContext) {
        val eventListener = gxTemplateContext.templateData?.eventListener
        if (gxTemplateContext.containers.isNotEmpty() && eventListener !is GXIManualExposureEventListener) {
            gxTemplateContext.templateData?.eventListener =
                object : GXIManualExposureEventListener {
                    override fun onGestureEvent(gxGesture: GXTemplateEngine.GXGesture) {
                        eventListener?.onGestureEvent(gxGesture)
                    }

                    override fun onScrollEvent(gxScroll: GXTemplateEngine.GXScroll) {
                        eventListener?.onScrollEvent(gxScroll)
                        if (gxTemplateContext.isAppear) {
                            if (GXTemplateEngine.GXScroll.TYPE_ON_SCROLL_STATE_CHANGED == gxScroll.type && gxScroll.state == RecyclerView.SCROLL_STATE_IDLE) {
                                GXContainerUtils.notifyOnAppear(gxTemplateContext)
                            }
                        }
                    }

                    override fun onAnimationEvent(gxAnimation: GXTemplateEngine.GXAnimation) {
                        eventListener?.onAnimationEvent(gxAnimation)
                    }
                }
        }
    }

    fun bindViewDataOnlyViewTree(gxTemplateContext: GXTemplateContext) {
        val rootNode = gxTemplateContext.rootNode
            ?: throw IllegalArgumentException("RootNode is null(bindViewDataOnlyViewTree) gxTemplateContext = $gxTemplateContext")

        // Update view layout
        GXViewTreeUpdater(gxTemplateContext, rootNode).build()

        // Update the view tree
        GXNodeTreeUpdater(gxTemplateContext).buildViewStyle()
    }
}