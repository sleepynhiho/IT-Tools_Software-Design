/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useState } from "react"; // Import React
import {
    Box,
    TextField,
    Button,
    Switch,
    Slider,
    Select,
    MenuItem,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
    FormHelperText, // Import FormHelperText
    useTheme,      // Import useTheme
    IconButton,    // Import IconButton
    InputAdornment // Import InputAdornment
} from "@mui/material";
import { ChromePicker } from "react-color";
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs'; // Adapter cho Day.js
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider'; // Provider
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker'; // Component chọn ngày giờ
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import dayjs from 'dayjs'; // Import dayjs

// --- Import Interfaces ---
import type { InputField } from '../data/pluginList'; // <-- Đảm bảo đường dẫn đúng

// --- Interface for Props ---
interface RenderInputProps {
    field: InputField; // Sử dụng kiểu InputField đã import
    value: any;
    onChange: (value: any) => void;
    disabled?: boolean;
}

// --- ColorPickerField Component (Giữ nguyên) ---
const ColorPickerField = ({ value, onChange, disabled = false }: { value: string; onChange: (val: string) => void; disabled?: boolean; }) => {
    // ... (code ColorPickerField giữ nguyên)
    const [showPicker, setShowPicker] = useState(false);
    const handleClosePicker = () => setShowPicker(false);
    const handleSwatchClick = (e: React.MouseEvent) => { e.stopPropagation(); if (!disabled) setShowPicker((prev) => !prev); };
    return (
        <Box position="relative" display="flex" flexDirection="column" gap={1} sx={{ width: '100%' }}>
            <Box sx={{ width: "100%", height: 40, borderRadius: 1, bgcolor: value, border: "1px solid #aaa", cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.5 : 1, position: 'relative', '&::after': disabled ? { content: '""', position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.1)', borderRadius: 'inherit', } : {} }} onClick={handleSwatchClick} />
            {showPicker && !disabled && (
                <>
                    <Box sx={{ position: 'fixed', top: 0, right: 0, bottom: 0, left: 0, zIndex: 9 }} onClick={handleClosePicker} />
                    <Box position="absolute" zIndex={10} top={50} left={0} >
                        <ChromePicker color={value} onChange={(color) => onChange(color.hex)} disableAlpha />
                    </Box>
                </>
            )}
            <TextField fullWidth type="text" value={value ?? ''} onChange={(e) => onChange(e.target.value)} sx={{ bgcolor: "#333", borderRadius: 1, '& .MuiInputBase-input': { color: 'white', fontFamily: 'monospace' }, '& .MuiOutlinedInput-root': { '& fieldset': { borderColor: '#555' }, '&:hover fieldset': { borderColor: '#777' }, '&.Mui-focused fieldset': { borderColor: '#36ad6a' }, }, }} InputLabelProps={{ shrink: true }} disabled={disabled} />
        </Box>
    );
};


// --- RenderInput Functional Component ---
const RenderInput: React.FC<RenderInputProps> = ({
    field,
    value,
    onChange,
    disabled = false
}) => {
    const theme = useTheme();
    const [showPassword, setShowPassword] = useState(false); // State cho nút ẩn/hiện password

    // --- Hàm tạo wrapper Box với width, label, helperText ---
    const renderWithWrapper = (inputElement: React.ReactNode, widthStyle: object) => (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0.5,
            ...widthStyle
        }}>
            {/* Render label nếu không phải switch (switch dùng FormControlLabel) */}
            {field.type !== 'switch' && field.label && (
                <Typography variant="body1" sx={{ fontWeight: 500, color: 'white' }}>
                    {field.label}
                </Typography>
            )}
            {inputElement}
            {field.helperText && (
                <FormHelperText sx={{ color: '#ccc', mt: 0.5 }}>
                    {field.helperText}
                </FormHelperText>
            )}
        </Box>
    );

    // --- Định nghĩa width ---
    const halfWidthCalc = `calc(50% - ${theme.spacing(1.25)})`; // 50% trừ nửa gap (giả sử gap là 2.5)
    const fullWidth = '100%';
    const autoWidth = 'auto';

    // --- Xử lý giá trị ngày giờ ---
    // Chuyển đổi giá trị từ string (nếu có) sang dayjs object hoặc null
    const handleDateTimeChange = (newValue: dayjs.Dayjs | null) => {
        // Gửi về dạng string ISO 8601 hoặc null/undefined tùy yêu cầu backend/state
        onChange(newValue ? newValue.toISOString() : null);
    };
    // Parse giá trị hiện tại thành dayjs object
    const currentDateTimeValue = value ? dayjs(value) : null;


    // --- Xử lý nút +/- cho number input ---
     const handleNumberIncrement = () => {
         const currentValue = Number(value ?? field.min ?? 0);
         const step = field.step ?? 1;
         let newValue = currentValue + step;
         if (field.max !== undefined && newValue > field.max) {
             newValue = field.max;
         }
         onChange(newValue);
     };

     const handleNumberDecrement = () => {
         const currentValue = Number(value ?? field.min ?? 0);
         const step = field.step ?? 1;
         let newValue = currentValue - step;
         if (field.min !== undefined && newValue < field.min) {
             newValue = field.min;
         }
         onChange(newValue);
     };

    // --- Switch cho các loại Input ---
    switch (field.type) {
        case "button": // Giữ lại nếu bạn có metadata định nghĩa nút riêng lẻ
            return renderWithWrapper(
                <Button variant="contained" onClick={() => onChange(field.value)} sx={{ bgcolor: "#36ad6a", '&:hover': { bgcolor: '#2a8a53' } }} disabled={disabled}>
                    {field.label}
                </Button>,
                { width: autoWidth } // Width tự động
                // Label đã nằm trong Button
            );

        case "switch": { // Đã có, dùng renderWithWrapper
            const trackHeight = 22; const thumbSize = 18; const desiredSwitchWidth = 40;
            const switchBasePadding = (trackHeight - thumbSize) / 2; const switchHeight = trackHeight + 2 * switchBasePadding;
            const finalCheckedTransform = `calc(${desiredSwitchWidth}px - ${thumbSize}px - ${2 * switchBasePadding}px)`;
            return renderWithWrapper(
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}> {/* Flex để label và switch gần nhau */}
                   <Typography variant="body1" sx={{ fontWeight: 500, color: 'white', mr: 1 }}>
                       {field.label}
                   </Typography>
                   <Switch checked={!!value} onChange={(e) => onChange(e.target.checked)} sx={{ /* sx switch */ width: `${desiredSwitchWidth}px`, height: `${switchHeight}px`, padding: 0, display: 'inline-flex', '& .MuiSwitch-track': { height: `${trackHeight}px`, borderRadius: `${trackHeight / 2}px`, opacity: 0.7, backgroundColor: '#aaa', boxSizing: 'border-box', width: '100%', transition: theme.transitions.create(['background-color'], { duration: theme.transitions.duration.shortest, }), }, '& .MuiSwitch-thumb': { width: `${thumbSize}px`, height: `${thumbSize}px`, color: 'white', boxShadow: 'none', boxSizing: 'border-box', transition: theme.transitions.create(['transform'], { duration: theme.transitions.duration.shortest, }), }, '& .MuiSwitch-switchBase': { padding: `${switchBasePadding}px`, boxSizing: 'border-box', transform: 'translateX(0px)', '&.Mui-checked': { transform: `translateX(${finalCheckedTransform})`, color: '#fff', '& + .MuiSwitch-track': { backgroundColor: '#36ad6a', opacity: 1, }, }, '&.Mui-disabled + .MuiSwitch-track': { opacity: 0.3 }, '&.Mui-disabled .MuiSwitch-thumb': { opacity: 0.5, color: '#ccc' }, }, }} disabled={disabled} />
                </Box>,
                { width: { xs: fullWidth, sm: halfWidthCalc } } // Chiếm nửa chiều rộng
            );
        }

        case "slider": // Đã có, dùng renderWithWrapper
             return renderWithWrapper(
                 <Box sx={{ width: '100%', px: 1 }}>
                     <Slider min={field.min ?? 0} max={field.max ?? 100} step={field.step ?? 1} value={typeof value === 'number' ? value : (field.default ?? field.min ?? 0)} onChange={(_, newVal) => onChange(newVal as number)} valueLabelDisplay="auto" sx={{ /* sx slider */ width: '100%', color: '#36ad6a', '& .MuiSlider-thumb': { backgroundColor: 'white', '&:hover, &.Mui-focusVisible': { boxShadow: `0px 0px 0px 8px rgba(54, 173, 106, 0.16)`, }, '&.Mui-active': { boxShadow: `0px 0px 0px 14px rgba(54, 173, 106, 0.16)`, }, }, '& .MuiSlider-rail': { color: '#aaa', opacity: 0.7, }, '& .MuiSlider-mark': { backgroundColor: '#ccc', height: 8, width: 1, marginTop: -3, }, '& .MuiSlider-markActive': { backgroundColor: 'currentColor', opacity: 1, }, }} disabled={disabled} marks={!!field.step && (field.max - field.min) / field.step <= 50} />
                 </Box>,
                 { width: { xs: fullWidth, sm: '65%' }, minWidth: '250px' } // Width lớn hơn
             );

        case "texarea": // Xử lý lỗi typo
        case "textarea":
        case "text": // Gộp text và textarea
            return renderWithWrapper(
                <TextField fullWidth multiline={field.type === "textarea" || field.type === "texarea"} value={value ?? ""} onChange={(e) => onChange(e.target.value)} sx={{ /* sx textfield */ bgcolor: "#333", borderRadius: 1, '& .MuiInputBase-input': { color: 'white' }, '& .MuiInputLabel-root': { color: '#ccc' }, '& .MuiOutlinedInput-root': { '& fieldset': { borderColor: '#555' }, '&:hover fieldset': { borderColor: '#777' }, '&.Mui-focused fieldset': { borderColor: '#36ad6a' }, }, '& .MuiInputBase-input::placeholder': { color: '#888', opacity: 1 }, }} disabled={disabled} rows={field.rows} placeholder={field.placeholder} />,
                { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
            );

        case "password": // MỚI
             return renderWithWrapper(
                 <TextField
                     fullWidth
                     type={showPassword ? 'text' : 'password'}
                     value={value ?? ""}
                     onChange={(e) => onChange(e.target.value)}
                     placeholder={field.placeholder}
                     disabled={disabled}
                     sx={{ /* sx textfield */ bgcolor: "#333", borderRadius: 1, '& .MuiInputBase-input': { color: 'white' }, '& .MuiOutlinedInput-root': { '& fieldset': { borderColor: '#555' }, '&:hover fieldset': { borderColor: '#777' }, '&.Mui-focused fieldset': { borderColor: '#36ad6a' }, }, }}
                     InputProps={{
                         endAdornment: (
                         <InputAdornment position="end">
                             <IconButton
                                aria-label="toggle password visibility"
                                onClick={() => setShowPassword((show) => !show)}
                                onMouseDown={(event) => event.preventDefault()} // Prevent focus loss on click
                                edge="end"
                                sx={{ color: 'grey' }}
                                disabled={disabled}
                             >
                             {showPassword ? <VisibilityOff /> : <Visibility />}
                             </IconButton>
                         </InputAdornment>
                         ),
                     }}
                 />,
                 { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
             );

        case "number": // Đã có, cập nhật để thêm nút +/-
             return renderWithWrapper(
                 <TextField
                     fullWidth
                     type="number"
                     value={value ?? ""}
                     onChange={(e) => { const numValue = e.target.value === '' ? '' : Number(e.target.value); onChange(numValue); }}
                     placeholder={field.placeholder}
                     disabled={disabled}
                     sx={{ /* sx textfield */ bgcolor: "#333", borderRadius: 1, '& .MuiInputBase-input': { color: 'white' }, '& .MuiOutlinedInput-root': { '& fieldset': { borderColor: '#555' }, '&:hover fieldset': { borderColor: '#777' }, '&.Mui-focused fieldset': { borderColor: '#36ad6a' }, }, }}
                     InputProps={{
                         inputProps: { min: field.min, max: field.max, step: field.step },
                         // Thêm nút +/- nếu metadata yêu cầu
                         startAdornment: field.buttons?.includes("minus") ? (
                             <InputAdornment position="start">
                                 <IconButton onClick={handleNumberDecrement} disabled={disabled || (field.min !== undefined && Number(value ?? field.min) <= field.min)} size="small" sx={{ color: 'grey' }}>
                                     <RemoveIcon fontSize="small" />
                                 </IconButton>
                             </InputAdornment>
                         ) : undefined,
                         endAdornment: field.buttons?.includes("plus") ? (
                             <InputAdornment position="end">
                                 <IconButton onClick={handleNumberIncrement} disabled={disabled || (field.max !== undefined && Number(value ?? field.min) >= field.max)} size="small" sx={{ color: 'grey' }}>
                                     <AddIcon fontSize="small" />
                                 </IconButton>
                             </InputAdornment>
                         ) : undefined,
                     }}
                 />,
                  { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
             );

        case "select": // Đã có, dùng renderWithWrapper
             return renderWithWrapper(
                 <Select fullWidth value={value ?? field.options?.[0]?.value ?? ""} onChange={(e) => onChange(e.target.value)} disabled={disabled} sx={{ /* sx select */ bgcolor: "#333", borderRadius: 1, color: 'white', '& .MuiSelect-icon': { color: '#ccc' }, '& .MuiOutlinedInput-notchedOutline': { borderColor: '#555' }, '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#777' }, '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#36ad6a' }, }} MenuProps={{ PaperProps: { sx: { bgcolor: '#444', color: 'white', '& .MuiMenuItem-root': { '&:hover': { bgcolor: '#555' }, '&.Mui-selected': { bgcolor: 'rgba(54, 173, 106, 0.3)', '&:hover': { bgcolor: 'rgba(54, 173, 106, 0.4)' } } }, }, }, }}>
                     {(field.options || []).map((option: any) => ( <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem> ))}
                 </Select>,
                  { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
             );

        case "color": // Đã có, dùng renderWithWrapper
              return renderWithWrapper(
                 <ColorPickerField value={value} onChange={onChange} disabled={disabled} />,
                 { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
              );

        case "datetime": // MỚI
             return renderWithWrapper(
                // Cần LocalizationProvider bao bọc ở cấp cao hơn (ví dụ: App.tsx hoặc nơi gần nhất)
                // <LocalizationProvider dateAdapter={AdapterDayjs}>
                     <DateTimePicker
                         value={currentDateTimeValue}
                         onChange={handleDateTimeChange}
                         disabled={disabled}
                         sx={{
                             width: '100%', // Chiếm hết wrapper Box
                             bgcolor: "#333",
                             borderRadius: 1,
                             '& .MuiInputBase-input': { color: 'white' },
                             '& .MuiOutlinedInput-root': { '& fieldset': { borderColor: '#555' }, '&:hover fieldset': { borderColor: '#777' }, '&.Mui-focused fieldset': { borderColor: '#36ad6a' }, },
                             '& .MuiSvgIcon-root': { color: '#ccc' }, // Màu icon lịch/đồng hồ
                         }}
                         // Có thể cần thêm props để custom format, views,...
                         // renderInput={(params) => <TextField {...params} fullWidth />} // Cách cũ, giờ dùng sx trực tiếp
                     />
                // </LocalizationProvider>,
                , { width: { xs: fullWidth, sm: halfWidthCalc } } // Nửa chiều rộng
             );

       case "buttons": // Giữ lại cho ToggleButtonGroup
            return renderWithWrapper(
                 <ToggleButtonGroup value={value} exclusive onChange={(_, newValue) => { if (newValue !== null) { onChange(newValue); } }} aria-label={field.label || "options"} disabled={disabled} sx={{ /* sx toggle button group */ display: 'inline-flex', flexWrap: 'wrap', '& .MuiToggleButtonGroup-grouped': { color: '#ccc', borderColor: '#555', '&:not(:first-of-type)': { borderLeftColor: '#555', marginLeft: '-1px' }, '&.Mui-selected': { color: 'white', backgroundColor: 'rgba(54, 173, 106, 0.4)', borderColor: '#36ad6a', '&:hover': { backgroundColor: 'rgba(54, 173, 106, 0.5)' }, }, '&:hover': { backgroundColor: '#444' }, '&.Mui-disabled': { color: '#777', borderColor: '#444' } }, }}>
                     {(field.options || []).map((option: any) => ( <ToggleButton key={option.value} value={option.value} aria-label={option.label}> {option.label} </ToggleButton> ))}
                 </ToggleButtonGroup>,
                  { width: { xs: fullWidth, sm: autoWidth } } // Tự động hoặc full width
             );

        default:
            // Render một thông báo lỗi hoặc text nếu không nhận dạng được type
            console.warn("Unknown input field type:", field.type, field);
            return renderWithWrapper(
                 <Typography sx={{ color: 'red', fontStyle: 'italic' }}>
                      Unknown input type: {field.type}
                 </Typography>,
                 { width: fullWidth }
            );
    }
};

export default RenderInput;