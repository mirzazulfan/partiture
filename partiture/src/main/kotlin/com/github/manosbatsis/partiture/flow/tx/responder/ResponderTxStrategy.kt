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
package com.github.manosbatsis.partiture.flow.tx.responder

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.delegate.responder.PartitureResponderFlowDelegate
import net.corda.core.flows.SignTransactionFlow

/** Responder flow delegate. */
interface ResponderTxStrategy : PartitureResponderFlowDelegate {

    /** Convenient chain method */
    fun setClientFlow(clientFlow: PartitureResponderFlow): ResponderTxStrategy {
        this.clientFlow = clientFlow;
        return this
    }

    /** Create a [SignTransactionFlow] with the appropriate verification checks. */
    @Suspendable
    fun createSignTransactionFlow(): SignTransactionFlow
}