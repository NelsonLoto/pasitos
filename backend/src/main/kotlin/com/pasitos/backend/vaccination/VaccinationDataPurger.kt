package com.pasitos.backend.vaccination

import com.pasitos.backend.child.ChildDataPurger
import java.util.UUID

/** Al eliminarse el perfil de un niño se eliminan sus vacunas (RN-HIJ-08). */
class VaccinationDataPurger(
    private val appliedDoses: AppliedDoseRepository,
    private val otherVaccines: OtherVaccineRepository,
) : ChildDataPurger {

    override fun purge(childId: UUID) {
        appliedDoses.purgeChild(childId)
        otherVaccines.purgeChild(childId)
    }
}
