/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.sagres

import androidx.annotation.RestrictTo
import kotlin.IllegalArgumentException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Constants {
    /**
     * The Sagres Server Constants
     * This provides a map of all attributes needed for UNES to connect to SAGRES.
     *
     * BASE_URL: The base address of the site
     * LOGIN_VIEW_STATE: A random generated string encoded in base64. You can get your one by inspecting the SAGRES login page it is the __VIEWSTATE variable
     * LOGIN_VW_STT_GEN: Another generated string. You can find it by the __VIEWSTATEGENERATOR variable
     * LOGIN_VIEW_VALID: Random generated string. It's variable name on HTML is __EVENTVALIDATION
     */
    private val SAGRES_SERVER_CONSTANTS = mapOf(
        "UEFS" to mapOf(
            "BASE_URL" to "http://academico2.uefs.br/Portal",
            "LOGIN_VIEW_STATE" to "/wEPDwUKMTc5MDkxMTc2NA9kFgJmD2QWBAIBD2QWDAIEDxYCHgRocmVmBT1+L0FwcF9UaGVtZXMvTmV3VGhlbWUvQWNlc3NvRXh0ZXJuby5jc3M/ZnA9NjM2Mzk4MDY3NDQwMDAwMDAwZAIFDxYCHwAFOH4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Db250ZXVkby5jc3M/ZnA9NjM2Mzk4MDY3NDQwMDAwMDAwZAIGDxYCHwAFOX4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Fc3RydXR1cmEuY3NzP2ZwPTYzNjIxNDcxMjMwMDAwMDAwMGQCBw8WAh8ABTl+L0FwcF9UaGVtZXMvTmV3VGhlbWUvTWVuc2FnZW5zLmNzcz9mcD02MzYyMTQ3MTIzMDAwMDAwMDBkAggPFgIfAAU2fi9BcHBfVGhlbWVzL05ld1RoZW1lL1BvcFVwcy5jc3M/ZnA9NjM2MjE0NzEyMzAwMDAwMDAwZAIJDxYCHwAFWC9Qb3J0YWwvUmVzb3VyY2VzL1N0eWxlcy9BcHBfVGhlbWVzL05ld1RoZW1lL05ld1RoZW1lMDEvZXN0aWxvLmNzcz9mcD02MzYxMDU4MjY2NDAwMDAwMDBkAgMPZBYEAgcPDxYEHgRUZXh0BQ1TYWdyZXMgUG9ydGFsHgdWaXNpYmxlaGRkAgsPZBYGAgEPDxYCHwJoZGQCAw88KwAKAQAPFgIeDVJlbWVtYmVyTWVTZXRoZGQCBQ9kFgICAg9kFgICAQ8WAh4LXyFJdGVtQ291bnRmZGTS+Y3bntF2UZMwIIXP8cpv13rKAw==",
            "LOGIN_VW_STT_GEN" to "BB137B96",
            "LOGIN_VIEW_VALID" to "/wEdAATbze7D9s63/L1c2atT93YlM4nqN81slLG8uEFL8sVLUjoauXZ8QTl2nEJmPx53FYhjUq3W1Gjeb7bKHHg4dlob4GWO7EiBlTRJt8Yw8hywpn30EZA="
        ),
        "UESC" to mapOf(
            "BASE_URL" to "http://www.prograd.uesc.br/PortalSagres",
            "LOGIN_VIEW_STATE" to "/wEPDwUKMTc5MDkxMTc2NA9kFgJmD2QWBAIBD2QWDAIEDxYCHgRocmVmBT1+L0FwcF9UaGVtZXMvTmV3VGhlbWUvQWNlc3NvRXh0ZXJuby5jc3M/ZnA9NjM2NjM3MzU3OTgwMDAwMDAwZAIFDxYCHwAFOH4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Db250ZXVkby5jc3M/ZnA9NjM2NjM3MzU3OTgwMDAwMDAwZAIGDxYCHwAFOX4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Fc3RydXR1cmEuY3NzP2ZwPTYzNjYzNzM1Nzk4MDAwMDAwMGQCBw8WAh8ABTl+L0FwcF9UaGVtZXMvTmV3VGhlbWUvTWVuc2FnZW5zLmNzcz9mcD02MzY2MzczNTc5ODAwMDAwMDBkAggPFgIfAAU2fi9BcHBfVGhlbWVzL05ld1RoZW1lL1BvcFVwcy5jc3M/ZnA9NjM2NjM3MzU3OTgwMDAwMDAwZAIJDxYCHwAFXi9Qb3J0YWxTYWdyZXMvUmVzb3VyY2VzL1N0eWxlcy9BcHBfVGhlbWVzL05ld1RoZW1lL05ld1RoZW1lMDEvZXN0aWxvLmNzcz9mcD02MzU5ODc0MDQ1MDAwMDAwMDBkAgMPZBYEAgcPDxYEHgRUZXh0BQ1TYWdyZXMgUG9ydGFsHgdWaXNpYmxlaGRkAgsPZBYGAgEPDxYCHwJoZGQCAw88KwAKAQAPFgIeDVJlbWVtYmVyTWVTZXRoZGQCBQ9kFgICAg9kFgICAQ8WAh4LXyFJdGVtQ291bnRmZGQinLLjRIyhdDAeQdsQFI4yEn7UBw==",
            "LOGIN_VW_STT_GEN" to "065EA922",
            "LOGIN_VIEW_VALID" to "/wEdAATEfAKci9KTCh4ou3w/C6rnM4nqN81slLG8uEFL8sVLUjoauXZ8QTl2nEJmPx53FYhjUq3W1Gjeb7bKHHg4dlobhmIrQ+4CIRu5sTfSTNFJmT7g9ok="
        ),
        "UNEB" to mapOf(
            "BASE_URL" to "http://www.portalacademico.uneb.br/PortalSagres",
            "LOGIN_VIEW_STATE" to "/wEPDwUKMTU3NjQ5NTUyNw9kFgJmD2QWBAIBD2QWDAIEDxYCHgRocmVmBT1+L0FwcF9UaGVtZXMvTmV3VGhlbWUvQWNlc3NvRXh0ZXJuby5jc3M/ZnA9NjM2ODQzNTM2OTgwMDAwMDAwZAIFDxYCHwAFOH4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Db250ZXVkby5jc3M/ZnA9NjM2ODQzNTM2OTgwMDAwMDAwZAIGDxYCHwAFOX4vQXBwX1RoZW1lcy9OZXdUaGVtZS9Fc3RydXR1cmEuY3NzP2ZwPTYzNjg0MzUzNjk4MDAwMDAwMGQCBw8WAh8ABTl+L0FwcF9UaGVtZXMvTmV3VGhlbWUvTWVuc2FnZW5zLmNzcz9mcD02MzY4NDM1MzcwMDAwMDAwMDBkAggPFgIfAAU2fi9BcHBfVGhlbWVzL05ld1RoZW1lL1BvcFVwcy5jc3M/ZnA9NjM2ODQzNTM3MDAwMDAwMDAwZAIJDxYCHwAFXi9Qb3J0YWxTYWdyZXMvUmVzb3VyY2VzL1N0eWxlcy9BcHBfVGhlbWVzL05ld1RoZW1lL05ld1RoZW1lMDEvZXN0aWxvLmNzcz9mcD02MzYxMDU4MjY2NDAwMDAwMDBkAgMPZBYEAgcPDxYEHgRUZXh0BQ1TYWdyZXMgUG9ydGFsHgdWaXNpYmxlaGRkAgsPZBYGAgEPDxYCHwJoZGQCAw88KwAKAQAPFgIeDVJlbWVtYmVyTWVTZXRoZGQCBQ9kFgRmDxYCHwJnZAICD2QWAgIBDxYCHgtfIUl0ZW1Db3VudAIBFgJmD2QWAgIBDxYCHgVzdHlsZQUObWF4LXdpZHRoOjRjbTsWBgIBDw8WAh8BBQ1TYWdyZXMgTW9iaWxlZGQCAw8PFgQeCEltYWdlVXJsBSx+L1Jlc291cmNlcy9JbWFnZXMvUVJDb2Rlcy9TYWdyZXMgTW9iaWxlLmdpZh4HVG9vbFRpcAUpaHR0cDovL3d3dy5wb3J0YWxhY2FkZW1pY28udW5lYi5ici9tb2JpbGUWAh8FBRVoZWlnaHQ6NGNtO3dpZHRoOjRjbTtkAgUPDxYCHwEFKWh0dHA6Ly93d3cucG9ydGFsYWNhZGVtaWNvLnVuZWIuYnIvbW9iaWxlZGRk6KbwVNQIRRanfYZS7fyfM5bX7WM=",
            "LOGIN_VW_STT_GEN" to "065EA922",
            "LOGIN_VIEW_VALID" to "/wEdAAQcFb3kXjnQ0ak1f3DTxtR0M4nqN81slLG8uEFL8sVLUjoauXZ8QTl2nEJmPx53FYhjUq3W1Gjeb7bKHHg4dlob2AnNAnxUHibEnTy5EZoeXTlpFwQ="
        )
    )

    // Sagres Default Endpoints
    private val SAGRES_ENDPOINTS = mapOf(
        "SAGRES_LOGIN_PAGE" to "__REPLACE__UNES__/Acesso.aspx",
        "SAGRES_MESSAGES_PAGE" to "__REPLACE__UNES__/Modules/Diario/Aluno/Consultas/Recados.aspx",
        "SAGRES_GRADE_PAGE" to "__REPLACE__UNES__/Modules/Diario/Aluno/Relatorio/Boletim.aspx",
        "SAGRES_DIARY_PAGE" to "__REPLACE__UNES__/Modules/Diario/Aluno/Default.aspx",
        "SAGRES_CLASS_PAGE" to "__REPLACE__UNES__/Modules/Diario/Aluno/Classe/ConsultaAulas.aspx",
        "SAGRES_GRADE_ANY" to "__REPLACE__UNES__/Modules/Diario/Aluno/Relatorio/Boletim.aspx?op=notas",
        "SAGRES_ENROLL_CERT" to "__REPLACE__UNES__/Modules/Diario/Aluno/Relatorio/ComprovanteMatricula.aspx",
        "SAGRES_FLOWCHART" to "__REPLACE__UNES__/Modules/Diario/Aluno/Relatorio/FluxogramaAluno.aspx",
        "SAGRES_HISTORY" to "__REPLACE__UNES__/Modules/Diario/Aluno/Relatorio/HistoricoEscolar.aspx",
        "SAGRES_DEMAND_OFFERS" to "__REPLACE__UNES__/Modules/Diario/Aluno/Matricula/Demanda.aspx",
        "SAGRES_REQUESTED_SERVICES" to "__REPLACE__UNES__/Modules/Diario/Aluno/Academico/SolicitacaoServico.aspx",
        "SAGRES_ALL_DISCIPLINES_PAGE" to "__REPLACE__UNES__/Modules/Diario/Aluno/Classe/SelecaoClasse.aspx?redirect=__REPLACE__UNES__/Modules/Diario/Aluno/Classe/ConsultaAulas.aspx"
    )

    @JvmStatic
    val SUPPORTED_INSTITUTIONS = SAGRES_SERVER_CONSTANTS.map { it.key }.toTypedArray()

    @JvmStatic
    val WIFI_PROXY_NAMES = arrayListOf(
        "UEFS_VISITANTES"
    )

    @JvmStatic
    private fun getPage(institution: String): String {
        val constants = SAGRES_SERVER_CONSTANTS[institution.toUpperCase()]
        return constants?.get("BASE_URL") ?: throw IllegalArgumentException("This institution is not supported")
    }

    @JvmStatic
    fun getUrl(endpoint: String): String {
        val institution = SagresNavigator.instance.getSelectedInstitution()
        val base = getPage(institution)
        val path = SAGRES_ENDPOINTS[endpoint.toUpperCase()] ?: throw IllegalArgumentException("This endpoint is not supported")
        return path.replace("__REPLACE__UNES__", base)
    }

    @JvmStatic
    fun getParameter(parameter: String): String {
        val institution = SagresNavigator.instance.getSelectedInstitution()
        val constants = SAGRES_SERVER_CONSTANTS[institution.toUpperCase()]
        return constants?.get(parameter) ?: throw IllegalArgumentException("This parameter is not supported")
    }
}
