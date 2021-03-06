/*
 *     Partiture: a compact component framework for your Corda apps
 *     Copyright (C) 2018 Manos Batsis
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package com.github.manosbatsis.partiture.flow

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowConverterDelegate
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.OutputConverter
import com.github.manosbatsis.partiture.flow.lifecycle.SimpleInitiatingLifecycle
import com.github.manosbatsis.partiture.flow.tx.initiating.SimpleTxStrategy
import com.github.manosbatsis.partiture.flow.tx.initiating.TxStrategy
import com.github.manosbatsis.partiture.flow.util.PartitureUtilsFlowLogic
import net.corda.core.flows.FlowLogic
import net.corda.core.utilities.ProgressTracker

/**
 * Base initiating [FlowLogic] implementation for flow composition
 */
open class PartitureFlow<IN, OUT>(
        val input: IN,
        val txStrategy: TxStrategy = SimpleTxStrategy(),
        val inputConverter: InputConverter<IN>? = null,
        val outputConverter: OutputConverter<OUT>? = null
) : PartitureUtilsFlowLogic<OUT>() {

    /** Obtain the appropriate progressTracker from the given [TxStrategy] */
    override val progressTracker = txStrategy.progressTracker

    lateinit var callContext: CallContext

    /**
     * Perform initialization. [CallContext] will not be available at this point.
     * Default implementation is NO-OP.
     */
    @Suspendable
    open fun initialize() { /* NO-OP */
    }

    /**
     * Convert the given `input` to Initialize the flow's [CallContext] with all items relevant to transaction(s):
     * Notary, input/output/reference states, attachments, commands and so on.
     *
     * The default implementation requires a non-null [InputConverter] parameter
     * via the flow's constructor. Either provide one or override the method to convert manually.
     */
    @Suspendable
    open fun processInput(): CallContext {
        return convert(inputConverter, input, "Input")
    }

    /**
     * Called after [com.github.manosbatsis.partiture.flow.PartitureFlow.processInput] completes
     * [CallContext] initialization. Default implementation is NO-OP.
     */
    @Suspendable
    open fun postProcessInput() { /* NO-OP */
    }

    /**
     * Produce an instance of the desired type for flow `OUT`.
     *
     * The default implementation requires a non-null [OutputConverter] parameter
     * via the flow's constructor to convert the [CallContext]. Either provide one
     * or override the method to convert manually.
     */
    @Suspendable
    fun processOutput(): OUT {
        return convert(outputConverter, callContext, "Output")
    }

    @Suspendable
    final override fun call(): OUT {
        progressTracker.currentStep = SimpleInitiatingLifecycle.INITIALIZE
        initialize()
        // Initialize the input/call context
        progressTracker.currentStep = SimpleInitiatingLifecycle.PROCESS_INPUT
        this.callContext = processInput()
        // Perform any post-processing of the input/call context
        progressTracker.currentStep = SimpleInitiatingLifecycle.POST_PROCESS_INPUT
        this.callContext = processInput()
        // Delegate processing to txStrategy
        progressTracker.currentStep = SimpleInitiatingLifecycle.EXECUTE_TRANSACTIONS
        txStrategy.setClientFlow(this).execute()
        // Perform any post-processing of transactions
        progressTracker.currentStep = SimpleInitiatingLifecycle.POST_EXECUTE_TRANSACTIONS
        // Process and return output
        progressTracker.currentStep = SimpleInitiatingLifecycle.PROCESS_OUTPUT
        val output = this.processOutput()
        // Update tracker to finished/DONE
        progressTracker.currentStep = ProgressTracker.DONE
        return output
    }

    @Suspendable
    private fun <I, O> convert(converter: PartitureFlowConverterDelegate<I, O>?, input: I, sourceName: String): O {
        return if (converter != null) converter.setClientFlow(this).convert(input)
        else throw NotImplementedError("This convert${sourceName}() implementation requires an ${sourceName}ConverterDelegate. " +
                "Either provide one in the flow's constructor or override the method to convert manually.")
    }
}