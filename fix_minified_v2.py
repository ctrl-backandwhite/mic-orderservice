#!/usr/bin/env python3
"""Fix minified Java controller files by restoring whitespace and newlines. v2"""
import re
import os
import glob

CONTROLLER_DIR = "/home/nexaplaform/Documentos/ecomerce/back/mic-orderservice/src/main/java/com/backandwhite/api/controller"

def is_minified(content):
    """Check if a file appears to be minified."""
    return 'packagecom.' in content or 'importcom.' in content or 'importio.' in content

def protect_strings(content):
    """Replace string literals with placeholders to avoid mangling them."""
    strings = []
    def replacer(m):
        strings.append(m.group(0))
        return f'__STR{len(strings)-1}__'
    # Match double-quoted strings (handling escaped quotes)
    content = re.sub(r'"(?:[^"\\]|\\.)*"', replacer, content)
    return content, strings

def restore_strings(content, strings):
    """Restore string literals from placeholders."""
    for i, s in enumerate(strings):
        content = content.replace(f'__STR{i}__', s)
    return content

def unminify_java(content):
    """Main unminification function."""
    
    # Step 0: Protect string literals
    content, strings = protect_strings(content)
    
    # Step 1: Fix keyword spacing - add space after Java keywords before identifiers
    keywords = [
        'package', 'import', 'public', 'private', 'protected', 'final', 'static',
        'abstract', 'class', 'interface', 'enum', 'extends', 'implements',
        'return', 'new', 'if', 'else', 'try', 'catch', 'throw', 'throws',
        'void', 'int', 'long', 'boolean', 'double', 'float', 'short', 'byte', 'char',
        'super', 'this', 'for', 'while', 'switch', 'case', 'break', 'continue',
        'default', 'synchronized', 'volatile', 'transient', 'native', 'instanceof',
        'null', 'true', 'false',
    ]
    for kw in sorted(keywords, key=len, reverse=True):
        content = re.sub(r'\b' + re.escape(kw) + r'(?=[a-zA-Z])', kw + ' ', content)
    
    # Step 2: Split concatenated imports line (package + imports on one line)
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if ('package ' in line and 'import ' in line) or (line.count('import ') > 1):
            parts = line.split(';')
            for p in parts:
                p = p.strip()
                if p:
                    new_lines.append(p + ';')
        else:
            new_lines.append(line)
    content = '\n'.join(new_lines)
    
    # Step 3: Fix annotation spacing
    # @RequestBody immediately followed by type name
    content = re.sub(r'@RequestBody([A-Z])', r'@RequestBody \1', content)
    # @PathVariable immediately followed by type name or identifier
    content = re.sub(r'@PathVariable([A-Z])', r'@PathVariable \1', content)
    content = re.sub(r'@PathVariable(String\b)', r'@PathVariable \1', content)
    # @RequestParam immediately followed by type name
    content = re.sub(r'@RequestParam([A-Z])', r'@RequestParam \1', content)
    
    # Step 4: Fix common Java type + variable name merges
    # String, Boolean, Integer, BigDecimal, etc. followed by lowercase variable
    wrapper_types = ['String', 'Boolean', 'Integer', 'Long', 'Double', 'Float',
                     'BigDecimal', 'Object', 'Void']
    for t in wrapper_types:
        content = re.sub(r'\b' + re.escape(t) + r'(?=[a-z])', t + ' ', content)
    
    # Fix generics closing > followed by lowercase variable name
    content = re.sub(r'>([a-z])', r'> \1', content)
    
    # Fix specific project types followed by lowercase variable name
    # These are CamelCase types where the variable starts with lowercase
    project_types = [
        # Use cases and mappers
        'CartUseCase', 'CartApiMapper', 'CouponUseCase', 'CouponApiMapper',
        'InvoiceUseCase', 'InvoiceApiMapper', 'OrderUseCase', 'OrderApiMapper',
        'ReturnUseCase', 'ReturnApiMapper', 'ShippingTaxUseCase', 'ShippingTaxApiMapper',
        'TrackingUseCase', 'TrackingApiMapper',
        # Models
        'Cart', 'CartItem', 'Coupon', 'Invoice', 'Order', 'OrderStats',
        'ReturnRequest', 'ShippingCarrier', 'ShippingRule', 'TaxRule',
        'TrackingEvent',
        # DTOs in
        'CartItemDtoIn', 'CartItemQuantityDtoIn', 'CartMergeDtoIn',
        'CouponDtoIn', 'ValidateCouponDtoIn',
        'InvoiceDtoIn', 'CreateOrderDtoIn', 'CancelOrderDtoIn', 'UpdateOrderStatusDtoIn',
        'ReturnRequestDtoIn', 'UpdateReturnStatusDtoIn',
        'ShippingCarrierDtoIn', 'ShippingRuleDtoIn', 'TaxRuleDtoIn',
        'TrackingEventDtoIn',
        # DTOs out
        'CartDtoOut', 'CartItemDtoOut',
        'CouponDtoOut', 'CouponValidationDtoOut',
        'InvoiceDtoOut', 'OrderDtoOut', 'OrderStatsDtoOut',
        'ReturnRequestDtoOut',
        'ShippingCarrierDtoOut', 'ShippingOptionsDtoOut', 'ShippingRuleDtoOut',
        'TaxCalculationDtoOut', 'TaxRuleDtoOut',
        'TrackingEventDtoOut',
        # Collections
        'HashMap', 'Map', 'List',
        # Spring/other
        'PaginationDtoOut', 'ResponseEntity',
    ]
    for t in sorted(project_types, key=len, reverse=True):
        # Type followed by lowercase = type + space + variable
        content = re.sub(r'\b' + re.escape(t) + r'(?=[a-z])', t + ' ', content)
    
    # Step 5: Fix line structure
    # Add newlines after ';' when followed by uppercase (new statement) 
    content = re.sub(r';([A-Z])', r';\n\1', content)
    # Add newlines after ';' when followed by keywords (return, if, etc.)
    content = re.sub(r';(return )', r';\n\1', content)
    content = re.sub(r';(if )', r';\n\1', content)
    content = re.sub(r';(try )', r';\n\1', content)
    content = re.sub(r';(for )', r';\n\1', content)
    content = re.sub(r';(while )', r';\n\1', content)
    # Add newlines after ';' before '}'
    content = re.sub(r';(\})', r';\n\1', content)
    # Add newlines after ';' before lowercase statement starters (method calls etc)
    content = re.sub(r';(cart[A-Z])', r';\n\1', content)
    content = re.sub(r';(coupon[A-Z])', r';\n\1', content)
    content = re.sub(r';(invoice[A-Z])', r';\n\1', content)
    content = re.sub(r';(order[A-Z])', r';\n\1', content)
    content = re.sub(r';(return [A-Z])', r';\n\1', content)
    content = re.sub(r';(shipping[A-Z])', r';\n\1', content)
    content = re.sub(r';(tracking[A-Z])', r';\n\1', content)
    
    # Add newline after '{' when followed by content (but not already newline)
    content = re.sub(r'\{(?!\n)([A-Za-z])', r'{\n\1', content)
    
    # Fix ')public class' -> ')\npublic class'
    content = re.sub(r'\)(\s*)public class ', r')\npublic class ', content)
    
    # Fix ')public ' for method signatures -> ')\npublic '
    content = re.sub(r'\)(\s*)public ', r')\npublic ', content)
    
    # Fix '}catch' -> '} catch'
    content = re.sub(r'\}catch', '} catch', content)
    
    # Fix '!=null' -> '!= null'
    content = re.sub(r'!=\s*null\b', '!= null', content)
    content = re.sub(r'==\s*null\b', '== null', content)
    content = re.sub(r'==0\b', '== 0', content)
    
    # Fix ');\n' followed by lowercase method call on new statement
    content = re.sub(r'\);\n([a-z])', r');\n\1', content)  # already has newline, that's fine
    
    # Step 6: Restore string literals
    content = restore_strings(content, strings)
    
    return content


def process_file(filepath):
    """Process a single Java file."""
    print(f"Processing: {os.path.basename(filepath)}")
    with open(filepath, 'r') as f:
        original = f.read()
    
    # Read again fresh from disk (might have been modified by v1)
    # Check if it looks properly formatted already (has proper package statement)
    if original.startswith('package ') and '\nimport ' in original[:200]:
        # Might already be formatted - but could still have issues in body
        # Check for remaining issues
        if '@RequestBody' not in original or '@RequestBody ' in original.split('@RequestBody')[1][:1] if len(original.split('@RequestBody')) > 1 else True:
            pass  # might be okay
    
    fixed = unminify_java(original)
    
    if fixed != original:
        with open(filepath, 'w') as f:
            f.write(fixed)
        print(f"  -> Fixed!")
    else:
        print(f"  -> No changes needed.")


def main():
    files = glob.glob(os.path.join(CONTROLLER_DIR, "*.java"))
    print(f"Found {len(files)} controller files\n")
    for f in sorted(files):
        process_file(f)
    print("\nDone!")


if __name__ == "__main__":
    main()
