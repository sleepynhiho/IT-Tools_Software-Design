import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import Home from "./pages/Home";
import theme from "./theme/theme";
// import TokenGenerator from "./pages/TokenGenerator";
// import HashText from "./pages/HashText";
import MainLayout from "./layouts/MainLayout";
// import ULIDGenerator from "./pages/ULIDGenerator";
// import PasswordStrength from "./pages/PasswordStrength";
// import IntegerBaseConverter from "./pages/IntegerBaseConverter";
// import XMLToJSONConverter from "./pages/XMLToJSONConverter";
// import JSONToXMLConverter from "./pages/JSONToXMLConverter";
import ToolRenderer from "./components/ToolRenderer";

function App() {
  return (
    <ThemeProvider theme={theme}>
      <Router>
        <MainLayout>
          <Routes>
            {/* <Route path="/" element={<Home />} /> */}
            <Route path="/" element={<Home />} />
            <Route path="/tools/:id" element={<ToolRenderer />} />

            {/* <Route path="/token-generator" element={<TokenGenerator />} />
            <Route path="/hash-text" element={<HashText />} />
            <Route path="/ulid-generator" element={<ULIDGenerator />} />
            <Route path="/password-strength" element={<PasswordStrength />} />
            <Route
              path="/integer-base-converter"
              element={<IntegerBaseConverter />}
            />

            <Route path="/xml-to-json" element={<XMLToJSONConverter />} /> */}
            {/* <Route path="/lorem-ipsum" element={<LoremIpsumGenerator />} /> */}
            {/* <Route path="/jwt-decoder" element={<JWTDecoder />} />
            {/* <Route path="/bip39-passphrase" element={<BIP39Passphrase />} />
          <Route path="/hmac-generator" element={<HMACGenerator />} />
          <Route
            path="/rsa-key-pair-generator"
            element={<RSAKeyPairGenerator />}
          />
          <Route
            path="/pdf-signature-checker"
            element={<PDFSignatureChecker />} */}
            {/* <Route path="*" element={<div>404 Not Found</div>} /> */}
          </Routes>
        </MainLayout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
