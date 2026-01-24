package tech.dokus.features.ai.prompts

/**
 * Expense category suggestion prompt.
 */
data object CategoryPrompt : AgentPrompt() {
    override val systemPrompt = Prompt(
        """
        Categorize this expense for a Belgian IT freelancer/SME.

        ## Categories (Belgian tax-relevant)

        | Category | Examples | Tax notes |
        |----------|----------|-----------|
        | OFFICE_SUPPLIES | Stationery, desk items, paper | 100% deductible |
        | HARDWARE | Computers, monitors, peripherals | >€500: depreciation required |
        | SOFTWARE | Licenses, SaaS, cloud services | 100% deductible |
        | TRAVEL | Flights, trains, hotels, accommodation | 100% deductible |
        | TRANSPORTATION | Fuel, car costs, local transport | Varies by type |
        | MEALS | Business meals, client entertainment | 69% deductible |
        | PROFESSIONAL_SERVICES | Accountant, lawyer, consultant | 100% deductible |
        | UTILITIES | Internet, phone, electricity | Home office: partial |
        | TRAINING | Courses, conferences, books, certs | 100% deductible |
        | MARKETING | Ads, website, domains, hosting | 100% deductible |
        | INSURANCE | Professional liability, health | Varies |
        | RENT | Office space, coworking | 100% deductible |
        | OTHER | Doesn't fit above | Case by case |

        ## Response Format

        Respond with a JSON object:
        {
            "suggestedCategory": "CATEGORY_NAME",
            "confidence": 0.0 to 1.0,
            "reasoning": "Brief explanation",
            "alternativeCategories": [
                {"category": "ALTERNATIVE", "confidence": 0.0 to 1.0}
            ]
        }
    """
    )
}