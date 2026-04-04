#!/usr/bin/env python3
"""Fix remaining issues after v1/v2 scripts. v3 targeted fixes."""
import re
import os
import glob

CONTROLLER_DIR = "/home/nexaplaform/Documentos/ecomerce/back/mic-orderservice/src/main/java/com/backandwhite/api/controller"

def fix_file(content):
    changed = False
    original = content
    
    # 1. Fix broken string literals (newline inserted inside quotes)
    # Pattern: string "... {\n ...}" needs to be rejoined  
    # e.g., "/items/{\nitemId}" -> "/items/{itemId}"
    # Also: "/items/{\n itemId}" -> "/items/{itemId}"
    content = re.sub(r'\{[\s]*\n[\s]*(itemId|orderId|id)\}', r'{\1}', content)
    
    # 2. Fix @RequestBody followed directly by type name
    content = re.sub(r'@RequestBody([A-Z][a-zA-Z]+)', r'@RequestBody \1', content)
    
    # 3. Fix @PathVariable followed directly by String/type
    content = re.sub(r'@PathVariable([A-Z][a-zA-Z]+)', r'@PathVariable \1', content)
    content = re.sub(r'@PathVariable String([a-z])', r'@PathVariable String \1', content)
    
    # 4. Fix DtoIn type + variable name merges  
    # CartItemDtoIndto -> CartItemDtoIn dto
    dto_types = [
        'CartItemDtoIn', 'CartItemQuantityDtoIn', 'CartMergeDtoIn',
        'CouponDtoIn', 'ValidateCouponDtoIn',
        'InvoiceDtoIn', 'CreateOrderDtoIn', 'CancelOrderDtoIn', 'UpdateOrderStatusDtoIn',
        'ReturnRequestDtoIn', 'UpdateReturnStatusDtoIn',
        'ShippingCarrierDtoIn', 'ShippingRuleDtoIn', 'TaxRuleDtoIn',
        'TrackingEventDtoIn',
    ]
    for dt in sorted(dto_types, key=len, reverse=True):
        content = re.sub(re.escape(dt) + r'([a-z])', dt + r' \1', content)
    
    # 5. Fix semicolon followed by "private" on same line
    content = re.sub(r';(private )', r';\n\1', content)
    
    # 6. Fix ")String " -> ") String " in annotation contexts
    content = re.sub(r'\)String ([a-z])', r') String \1', content)
    content = re.sub(r'\)int ([a-z])', r') int \1', content)
    content = re.sub(r'\)boolean ([a-z])', r') boolean \1', content)
    content = re.sub(r'\)Boolean ([a-z])', r') Boolean \1', content)
    
    # 7. Fix ")BigDecimal " 
    content = re.sub(r'\)BigDecimal ([a-z])', r') BigDecimal \1', content)
    
    # 8. Fix "false )String" -> "false) String" - no, that's wrong context
    # Pattern: "required =false)String " -> "required = false) String "
    content = re.sub(r'=\s*false\)String ', '= false) String ', content)
    content = re.sub(r'=\s*false\)int ', '= false) int ', content)
    content = re.sub(r'=\s*false\)boolean ', '= false) boolean ', content)
    
    # 9. Fix missing space around = in "required =false" -> "required = false"
    # This is fine as-is for Java compilation but let's clean it
    
    # 10. Fix the remaining ;lowercase on same line statements
    # ;cartUseCase -> ;\ncartUseCase
    content = re.sub(r';(cartUseCase)', r';\n\1', content)
    content = re.sub(r';(couponUseCase)', r';\n\1', content)
    content = re.sub(r';(invoiceUseCase)', r';\n\1', content)
    content = re.sub(r';(orderUseCase)', r';\n\1', content)
    content = re.sub(r';(returnUseCase)', r';\n\1', content)
    content = re.sub(r';(shippingTaxUseCase)', r';\n\1', content)
    content = re.sub(r';(trackingUseCase)', r';\n\1', content)
    content = re.sub(r';(filters)', r';\n\1', content)
    
    # 11. Fix "=newHashMap" -> "= new HashMap"
    content = re.sub(r'=new ', '= new ', content)
    
    # 12. Fix ") {" already on same line followed by content  
    # Some ") {Cart..." should be ") {\nCart..."
    # Actually, the v2 script should have handled this. Let me check for remaining:
    content = re.sub(r'\) \{([A-Z])', r') {\n\1', content)
    content = re.sub(r'\) \{(cart[A-Z])', r') {\n\1', content)
    content = re.sub(r'\) \{(coupon[A-Z])', r') {\n\1', content)
    content = re.sub(r'\) \{(return )', r') {\n\1', content)
    content = re.sub(r'\) \{(try )', r') {\n\1', content)
    
    # 13. Fix ") {" followed by ;\n pattern already handled
    
    # Additional: Fix ";if " properly  
    content = re.sub(r';if \(', ';\nif (', content)
    
    if content != original:
        changed = True
    
    return content, changed


def main():
    files = glob.glob(os.path.join(CONTROLLER_DIR, "*.java"))
    print(f"Found {len(files)} controller files\n")
    for filepath in sorted(files):
        name = os.path.basename(filepath)
        with open(filepath, 'r') as f:
            content = f.read()
        fixed, changed = fix_file(content)
        if changed:
            with open(filepath, 'w') as f:
                f.write(fixed)
            print(f"  {name} -> Fixed!")
        else:
            print(f"  {name} -> No changes needed.")
    print("\nDone!")


if __name__ == "__main__":
    main()
