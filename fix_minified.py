#!/usr/bin/env python3
"""Fix minified Java controller files by restoring whitespace and newlines."""
import re
import os
import glob

CONTROLLER_DIR = "src/main/java/com/backandwhite/api/controller"

# Java keywords that need a space after them when followed by an identifier
KEYWORDS = [
    'package', 'import', 'public', 'private', 'protected', 'final', 'static',
    'abstract', 'class', 'interface', 'enum', 'extends', 'implements',
    'return', 'new', 'if', 'else', 'try', 'catch', 'throw', 'throws',
    'void', 'int', 'long', 'boolean', 'double', 'float', 'short', 'byte', 'char',
    'super', 'this', 'for', 'while', 'switch', 'case', 'break', 'continue',
    'default', 'synchronized', 'volatile', 'transient', 'native', 'instanceof',
    'null', 'true', 'false',
]

def is_minified(content):
    """Check if a file appears to be minified."""
    return 'packagecom.' in content or 'importcom.' in content or 'importio.' in content

def fix_keyword_spacing(text):
    """Add spaces after Java keywords when they're directly followed by an identifier char."""
    for kw in sorted(KEYWORDS, key=len, reverse=True):
        # keyword immediately followed by a letter (identifier start)
        pattern = r'\b' + re.escape(kw) + r'(?=[a-zA-Z])'
        text = re.sub(pattern, kw + ' ', text)
    return text

def fix_type_variable_spacing(text):
    """Add space between UpperCaseType and lowerCaseVariable patterns.
    e.g., CartUseCasecartUseCase -> CartUseCase cartUseCase
           Cartcart -> Cart cart
           BigDecimaldiscount -> BigDecimal discount
    """
    # Pattern: UppercaseWord immediately followed by lowercase word
    # We need to handle cases like "CartUseCase" (valid CamelCase) vs "CartUseCasecartUseCase" (type + var)
    # Strategy: Look for a lowercase letter followed by an uppercase letter, where splitting
    # would create a valid type + variable pair. But that's CamelCase...
    
    # Better strategy: look for patterns where:
    # - An uppercase letter is preceded by a lowercase letter (end of prev word)
    # - And followed by lowercase letters (start of var name)
    # But this would incorrectly split CamelCase...
    
    # The safest approach: target specific known patterns after certain contexts
    # After ';' or '{' or '}' at start of what looks like a declaration
    # e.g., ";Cartcart =" or "{Cartcart ="
    
    # Actually, let me use a different approach:
    # After fixing keywords, the remaining issues are Type+variable merges.
    # These happen in specific contexts:
    # 1. After ';' or '{' at the start of a statement
    # 2. After 'final ' (already has space from keyword fix)
    # 3. In method parameter declarations after ')' 
    
    # Let's handle: after 'final ' , we have TypeNamevarName -> TypeName varName
    text = re.sub(r'(final\s+)([A-Z][a-zA-Z0-9<>,\s?]*?)([a-z][a-zA-Z0-9]*\s*[=;,)])', 
                  lambda m: fix_final_decl(m), text)
    
    return text

def fix_final_decl(m):
    """Fix 'final TypeVarname' -> 'final Type varname'"""
    prefix = m.group(1)  # 'final '
    type_and_var = m.group(2) + m.group(3)
    # Already handled elsewhere, skip
    return m.group(0)

def split_imports_line(text):
    """Split the first line that has all imports concatenated."""
    lines = text.split('\n')
    if not lines:
        return text
    
    first_line = lines[0]
    if 'package' in first_line and 'import' in first_line:
        # Split on ';' and add newline after each
        parts = first_line.split(';')
        new_parts = []
        for p in parts:
            p = p.strip()
            if p:
                new_parts.append(p + ';')
        lines[0] = '\n'.join(new_parts)
    
    return '\n'.join(lines)

def fix_class_body_spacing(text):
    """Fix spacing issues inside class body where statements are concatenated."""
    
    # Fix UppercaseType + lowercaseVariable patterns in declarations/assignments
    # Pattern: after ; or { at the start of a statement
    # e.g., ";Cartcart =" -> ";\nCart cart ="
    # e.g., ";CartItemitem =" -> ";\nCartItem item ="
    # This is tricky because we need to distinguish CamelCase from Type+var
    
    # Approach: find patterns like "TypeName" followed by the same name but lowercase
    # Actually, let's look for: UpperCaseName immediately followed by lowerCaseName 
    # where lowerCaseName starts with the same root
    
    # Better: after ; or { if we see an Uppercase followed by a lowercase sequence
    # that makes sense as type varname
    
    # Let's use a comprehensive regex approach:
    # After a semicolon (and optional space), if we see UppercaseName+lowercaseName,
    # split them
    
    # First, handle the specific pattern: ")TypeNamevarName" after closing paren in method body
    # and ";TypeNamevarName" 
    
    # A simpler approach: find all "XxxYyyxxxYyy" patterns where the lowercase start
    # is clearly a variable name
    
    # Known type patterns in this codebase:
    types_in_code = [
        'Cart', 'CartItem', 'CartUseCase', 'CartApiMapper',
        'Coupon', 'CouponUseCase', 'CouponApiMapper', 'CouponValidationDtoOut',
        'Invoice', 'InvoiceUseCase', 'InvoiceApiMapper',
        'Order', 'OrderUseCase', 'OrderApiMapper', 'OrderStats',
        'ReturnRequest', 'ReturnUseCase', 'ReturnApiMapper',
        'ShippingCarrier', 'ShippingRule', 'ShippingTaxUseCase', 'ShippingTaxApiMapper',
        'ShippingOptionsDtoOut',
        'TaxRule',
        'TrackingEvent', 'TrackingUseCase', 'TrackingApiMapper',
        'BigDecimal', 'String', 'Map', 'HashMap', 'List',
        'PaginationDtoOut', 'ResponseEntity',
        'Boolean',
    ]
    
    # Generic approach: After certain delimiters, split CamelCase into Type + variable
    # Actually, let's just use a universal regex to split merged type+var names
    
    # The pattern is: an Uppercase word (type) immediately followed by a lowercase word (variable)
    # where the lowercase word is not part of the type's CamelCase
    
    # Key insight: In CamelCase, uppercase letters appear within a word (CartUseCase)
    # When type+var are merged: CartUseCasecartUseCase - there's a lowercase 'c' after 'e'
    # that starts a new identifier
    
    # Strategy: After known statement boundaries (;, {, }), look for Type+var merge
    # Pattern: a sequence like XxxYyy...xxxYyy where the lowercase after uppercase is a var start
    
    # Let's use the heuristic: if after a ; or { or } we see a pattern like
    # [A-Z][a-zA-Z]*[a-z][A-Z] followed by [a-z], that's a CamelCase split point
    # Wait no, that would mess up regular CamelCase.
    
    # Let me think about this differently. The issues are very specific:
    # 1. After ';' a new Java statement starts but there's no newline/space
    # 2. In that new statement, the type and the variable name are merged
    
    # Examples:
    # ");Cartcart =" -> ");\nCart cart ="
    # ");CartItemitem =" -> ");\nCartItem item ="
    # ");" is fine if followed by newline or }
    
    # Let me handle it line by line
    
    return text

def unminify_java(content):
    """Main unminification function."""
    
    if not is_minified(content):
        return content
    
    print("  -> File is minified, fixing...")
    
    # Step 1: Fix keyword spacing (this handles most merged keywords)
    content = fix_keyword_spacing(content)
    
    # Step 2: Split the concatenated imports line
    content = split_imports_line(content)
    
    # Step 3: Add newlines after ';' when followed by a Java statement (not in string literals or for-loops)
    # Handle semicolons inside the class body that are followed by uppercase (new statement)
    content = re.sub(r';([A-Z])', r';\n\1', content)
    
    # Step 4: Add newlines after '{' when followed by content on same line
    content = re.sub(r'\{([A-Za-z])', r'{\n\1', content)
    
    # Handle ';}' -> ';\n}'
    content = re.sub(r';(\})', r';\n\1', content)
    
    # Step 5: Fix type + variable name merges
    # After 'final ', TypeNamevarName -> TypeName varName
    # Pattern: 'final TypeNamvarNam' where a CamelCase type is followed by a lowercase identifier
    # We detect this by looking for the pattern: UpperCase+mixed -> lowercase+mixed
    # e.g., CartUseCasecartUseCase, Cartcart, BigDecimaldiscount
    
    # The general pattern: A CamelCase sequence where at some point, an uppercase letter
    # that was repeated/reflected in lowercase indicates the split point.
    # e.g., "CartUseCasecartUseCase" - 'c' after 'e' when we know 'Cart' starts with 'C'
    
    # Heuristic: Find sequences where a lowercase letter follows and we can identify
    # a well-known type or pattern
    
    # Actually, let me just handle common specific patterns:
    # UpperCamelCase immediately followed by lowerCamelCase that's a plausible variable name
    # The key signal: after a series of CamelCase, we see a lowercase letter that would
    # start a new identifier (because Java vars start with lowercase)
    
    # Pattern: (Foo)(bar) -> "Foo bar" when Foo is a type and bar is a variable
    # We need to find the split point in FooBar vs Foobar
    
    # Looking at actual patterns:
    # "Cartcart" - split at (Cart)(cart) 
    # "CartItemitem" - split at (CartItem)(item)
    # "CartUseCasecartUseCase" - split at (CartUseCase)(cartUseCase)
    # "CartApiMappercartApiMapper" - split at (CartApiMapper)(cartApiMapper)
    # "BigDecimaldiscount" - split at (BigDecimal)(discount)
    # "Stringstatus" - already handled by keyword fix? No, String is not a keyword...
    # Actually "String" is after "import" so... Let me check.
    # "StringnxAuth" -> "String nxAuth"
    # "StringuserId" -> "String userId"  
    # "StringsessionId" -> "String sessionId"
    # "StringsortBy" -> "String sortBy"
    # "Stringstatus" -> "String status"
    # "StringorderId" -> "String orderId"
    # "Stringsearch" -> "String search"
    # "Stringreason" -> "String reason"
    # "Stringregion" -> "String region"
    # "Stringcountry" -> "String country"
    # "Stringid" -> "String id"
    
    # And generics:
    # "Map<String,Object>filters" -> "Map<String, Object> filters"
    # "List<ShippingRule>rules" -> "List<ShippingRule> rules"
    # etc.
    
    # After generics, the closing > immediately followed by lowercase var name
    content = re.sub(r'>([a-z])', r'> \1', content)
    
    # Handle "String" + lowercase - String is not a Java keyword per se for our purposes
    content = re.sub(r'\bString([a-z])', r'String \1', content)
    content = re.sub(r'\bBoolean([a-z])', r'Boolean \1', content)
    content = re.sub(r'\bInteger([a-z])', r'Integer \1', content)
    content = re.sub(r'\bLong([a-z])', r'Long \1', content)
    content = re.sub(r'\bDouble([a-z])', r'Double \1', content)
    content = re.sub(r'\bObject([a-z])', r'Object \1', content)
    content = re.sub(r'\bBigDecimal([a-z])', r'BigDecimal \1', content)
    
    # Handle common types in the codebase where type name merges with variable name
    # Pattern: TypeName followed by the same name but starting lowercase
    # e.g., Cartcart, OrderUseCaseorderUseCase
    
    # Generic pattern: CamelCaseType immediately followed by lowerCamelCase variable
    # We can detect this when the pattern is TypeType where the lowercase start of var
    # matches the start of the type
    
    # Let's handle specific codebase types
    specific_types = [
        'Cart', 'CartItem', 'CartUseCase', 'CartApiMapper', 'CartDtoOut', 'CartItemDtoOut',
        'Coupon', 'CouponUseCase', 'CouponApiMapper', 'CouponValidationDtoOut',
        'Invoice', 'InvoiceUseCase', 'InvoiceApiMapper', 'InvoiceDtoOut',
        'Order', 'OrderUseCase', 'OrderApiMapper', 'OrderStats', 'OrderDtoOut', 'OrderStatsDtoOut',
        'ReturnRequest', 'ReturnUseCase', 'ReturnApiMapper', 'ReturnRequestDtoOut',
        'ShippingCarrier', 'ShippingRule', 'ShippingTaxUseCase', 'ShippingTaxApiMapper',
        'ShippingOptionsDtoOut', 'ShippingCarrierDtoOut', 'ShippingRuleDtoOut',
        'TaxRule', 'TaxCalculationDtoOut', 'TaxRuleDtoOut',
        'TrackingEvent', 'TrackingUseCase', 'TrackingApiMapper', 'TrackingEventDtoOut',
        'PaginationDtoOut', 'ResponseEntity',
        'HashMap', 'Map', 'List',
    ]
    
    for t in sorted(specific_types, key=len, reverse=True):
        # TypeName followed by lowercase identifier char (variable name start)
        pattern = re.escape(t) + r'(?=[a-z])'
        # But we need to avoid replacing inside a longer identifier
        # Use word boundary at the start of the type
        # Actually, we can't assume word boundary because it might appear after > or space already
        # Let's be more careful: only replace when preceded by a space, newline, or certain chars
        content = re.sub(r'(?<=[\s({,;>])' + re.escape(t) + r'(?=[a-z])', t + ' ', content)
        # Also after 'final ' 
        content = re.sub(r'(?<=final )' + re.escape(t) + r'(?=[a-z])', t + ' ', content)
    
    # Handle generic closings before types: ">Type..." already handled above with > pattern
    
    # Fix ")public" -> ")\npublic" for class declarations
    content = re.sub(r'\)public\s+class\s', ')\npublic class ', content)
    
    # Fix ") public class" at annotation endings
    content = re.sub(r'"\)\s*public\s+class\s', '")\npublic class ', content)
    
    # Fix missing newlines before 'return' when it appears after {
    # Already handled by the { newline rule above
    
    # Fix ";return " -> ";\nreturn "
    content = re.sub(r';(return )', r';\n\1', content)
    
    # Fix ";if " -> ";\nif "
    content = re.sub(r';(if )', r';\n\1', content)
    
    # Handle catch on same line: "}catch" -> "} catch"
    content = re.sub(r'\}catch', '} catch', content)
    
    # Handle "} catch" already there but maybe needs newline
    
    # Fix Void in return type: "Void>" patterns (should be fine)
    # Fix "ResponseEntity" appearing after method signature open
    
    # Fix ");" followed by lowercase on same line -> ");\n"  
    content = re.sub(r'\);([a-z])', r');\n\1', content)
    
    # Fix ";}" should be ";\n}"
    # Already handled above
    
    # Fix "= new" already handled by keyword fix
    # But "=new" might remain
    content = re.sub(r'=new\s', '= new ', content)
    content = re.sub(r'= new\s', '= new ', content) # normalize spaces
    
    # Fix "!=null" -> "!= null"  (already handled by keyword "null" spacing)
    # Actually, the keyword fix adds space after 'null' but not before
    content = re.sub(r'!=null\b', '!= null', content)
    content = re.sub(r'==null\b', '== null', content)
    content = re.sub(r'==0', '== 0', content)
    
    # Fix ")filters" etc. - after closing paren, lowercase var name
    # This happens in cast/generic contexts like "Map<String,Object>filters"
    # The > case is handled. But "(type)var" casts... let's skip those for now.
    
    # Fix missing space after annotation descriptions ending with ")
    # e.g., description = "...")publicclass -> description = "...")\npublic class
    # This should already be handled by the ")public class" fix above.
    
    # Fix "Void>" issue - ResponseEntity<Void>
    # Already fine
    
    # Fix type after ';' followed by lowercase
    # e.g., ";Coupon coupon" - the newline is added but we need to check type+var
    # After step 3, we have newlines after ; + uppercase. So "Coupon coupon" should be fine
    # if the space is added by specific_types handling
    
    # Fix some remaining patterns:
    # After "try {" -> newline
    # The "{" followed by content rule handles this
    
    # Fix ";Coupon" -> already has newline from step 3
    
    # Fix the specific pattern: annotations ending ) followed by "public" on same line
    # for methods like:
    # @Operation(summary = "...")public ResponseEntity
    content = re.sub(r'"\)public\s', '")\npublic ', content) # already covered above? Let's be safe
    content = re.sub(r'"\)\s*\n\s*public\s+class\s', '")\npublic class ', content)
    
    # Handle @NxUser and @NxAdmin annotations that may be on same line as method
    content = re.sub(r'(@Nx(?:User|Admin|Public))\s*public\s', r'\1\npublic ', content) # hmmm, these may not exist in these files
    
    # Fix methods: after ") {" should have newline if content follows
    # Already handled by the "{ followed by content" rule
    
    return content


def process_file(filepath):
    """Process a single Java file."""
    print(f"Processing: {filepath}")
    with open(filepath, 'r') as f:
        content = f.read()
    
    if not is_minified(content):
        print("  -> Already formatted, skipping.")
        return
    
    fixed = unminify_java(content)
    
    with open(filepath, 'w') as f:
        f.write(fixed)
    
    print("  -> Fixed!")


def main():
    files = glob.glob(os.path.join(CONTROLLER_DIR, "*.java"))
    print(f"Found {len(files)} controller files")
    for f in sorted(files):
        process_file(f)
    print("\nDone!")


if __name__ == "__main__":
    main()
