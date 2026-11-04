import io
import re
import logging
from typing import Optional
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from PIL import Image
import numpy as np

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="VeriMetra OCR Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize OCR engine
ocr_engine = None
try:
    from paddleocr import PaddleOCR
    ocr_engine = PaddleOCR(use_angle_cls=True, lang='en', show_log=False)
    logger.info("PaddleOCR initialized successfully")
except Exception as e:
    logger.warning(f"PaddleOCR initialization failed: {e}. OCR will return empty results.")


class ExtractedField(BaseModel):
    field_name: str
    suggested_value: str
    confidence: float


class ExtractionResult(BaseModel):
    raw_text: str
    extracted_fields: list[ExtractedField]
    extraction_status: str
    error_message: Optional[str] = None


ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "image/bmp", "image/tiff"}
MAX_SIZE = 10 * 1024 * 1024  # 10MB


def extract_fields_from_text(text: str) -> list[ExtractedField]:
    """Extract structured fields from raw OCR text using pattern matching."""
    fields = []
    lines = text.split('\n')
    full_text = ' '.join(lines)
    
    # MRP patterns
    mrp_patterns = [
        r'(?:MRP|M\.R\.P|Maximum Retail Price|Price)[:\s]*(?:Rs\.?|₹|INR)?\s*(\d+(?:\.\d{1,2})?)',
        r'(?:Rs\.?|₹|INR)\s*(\d+(?:\.\d{1,2})?)',
    ]
    for pattern in mrp_patterns:
        match = re.search(pattern, full_text, re.IGNORECASE)
        if match:
            fields.append(ExtractedField(
                field_name="mrp",
                suggested_value=match.group(1),
                confidence=0.7
            ))
            break
    
    # Net quantity patterns
    qty_patterns = [
        r'(?:Net\s*(?:Wt|Weight|Qty|Quantity|Content))[.:\s]*(\d+(?:\.\d+)?\s*(?:g|gm|gms|kg|ml|l|ltr|litre|liter|oz|lb))',
        r'(\d+(?:\.\d+)?\s*(?:g|gm|gms|kg|ml|l|ltr|litre|liter))\b',
    ]
    for pattern in qty_patterns:
        match = re.search(pattern, full_text, re.IGNORECASE)
        if match:
            value = match.group(1).strip()
            fields.append(ExtractedField(
                field_name="net_quantity",
                suggested_value=value,
                confidence=0.65
            ))
            break
    
    # Manufacturer patterns
    mfr_patterns = [
        r'(?:Mfg\.?\s*(?:by)?|Manufactured\s*by|Mfr|Packed\s*by|Packer)[:\s]*([A-Za-z][A-Za-z\s&.,]+?)(?:\n|,\s*[A-Z]|\d)',
        r'(?:Made\s*by)[:\s]*([A-Za-z][A-Za-z\s&.,]+?)(?:\n|,)',
    ]
    for pattern in mfr_patterns:
        match = re.search(pattern, full_text, re.IGNORECASE)
        if match:
            value = match.group(1).strip().rstrip(',.')
            if len(value) > 2:
                fields.append(ExtractedField(
                    field_name="manufacturer_name",
                    suggested_value=value,
                    confidence=0.6
                ))
            break
    
    # Country of origin
    country_patterns = [
        r'(?:Country\s*of\s*Origin|Made\s*in|Product\s*of|Origin)[:\s]*([A-Za-z][A-Za-z\s]+)',
    ]
    for pattern in country_patterns:
        match = re.search(pattern, full_text, re.IGNORECASE)
        if match:
            value = match.group(1).strip()
            if len(value) > 1:
                fields.append(ExtractedField(
                    field_name="country_of_origin",
                    suggested_value=value,
                    confidence=0.7
                ))
            break
    
    # Date patterns
    date_patterns = [
        (r'(?:Mfg\.?\s*(?:Date)?|Mfd|Manufacturing\s*Date|Pkg\.?\s*Date|Packing\s*Date)[:\s]*(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})', "manufacture_date"),
        (r'(?:Best\s*Before|Exp(?:iry)?\.?\s*(?:Date)?|Use\s*Before|BB)[:\s]*(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})', "expiry_date"),
    ]
    for pattern, field_name in date_patterns:
        match = re.search(pattern, full_text, re.IGNORECASE)
        if match:
            fields.append(ExtractedField(
                field_name=field_name,
                suggested_value=match.group(1),
                confidence=0.6
            ))
    
    return fields


@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "ocr_engine": "paddleocr" if ocr_engine else "unavailable",
        "service": "verimetra-ocr"
    }


@app.post("/extract", response_model=ExtractionResult)
async def extract_text(file: UploadFile = File(...)):
    # Validate content type
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported file type: {file.content_type}. Supported: {', '.join(ALLOWED_TYPES)}"
        )
    
    # Read file
    content = await file.read()
    if len(content) > MAX_SIZE:
        raise HTTPException(status_code=400, detail="File too large. Maximum size is 10MB.")
    
    if len(content) == 0:
        raise HTTPException(status_code=400, detail="Empty file uploaded.")
    
    # Try to open as image
    try:
        image = Image.open(io.BytesIO(content))
        image_array = np.array(image.convert('RGB'))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {str(e)}")
    
    # Run OCR
    if ocr_engine is None:
        return ExtractionResult(
            raw_text="",
            extracted_fields=[],
            extraction_status="OCR_UNAVAILABLE",
            error_message="OCR engine is not available. Please enter declarations manually."
        )
    
    try:
        results = ocr_engine.ocr(image_array, cls=True)
        
        if not results or not results[0]:
            return ExtractionResult(
                raw_text="",
                extracted_fields=[],
                extraction_status="NO_TEXT_FOUND",
                error_message="No text could be extracted from the image."
            )
        
        # Build raw text
        lines = []
        for line in results[0]:
            if line and len(line) >= 2:
                text = line[1][0] if isinstance(line[1], (list, tuple)) else str(line[1])
                lines.append(text)
        
        raw_text = '\n'.join(lines)
        extracted_fields = extract_fields_from_text(raw_text)
        
        return ExtractionResult(
            raw_text=raw_text,
            extracted_fields=extracted_fields,
            extraction_status="SUCCESS"
        )
    except Exception as e:
        logger.error(f"OCR extraction failed: {e}")
        return ExtractionResult(
            raw_text="",
            extracted_fields=[],
            extraction_status="EXTRACTION_FAILED",
            error_message=f"OCR extraction failed: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8090)
