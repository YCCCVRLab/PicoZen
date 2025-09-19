# YCCC VR Lab Server Integration

This version of PicoZen has been enhanced by the **York County Community College VR Lab** to include automatic connection to our curated VR app store.

## 🆕 What's New

### Automatic App Store Connection
- **YCCC VR Lab Server** option in sideload settings
- Automatic connection to `https://picozen-api.netlify.app`
- No manual IP configuration required
- Curated educational VR applications

### Featured Applications
- **UbiSim** - VR nursing simulation platform
- Additional educational VR apps coming soon
- All apps vetted for educational use

## 🚀 How to Use

### For Students & Educators:
1. Open PicoZen on your VR headset
2. Go to **Sideload** section
3. In **Settings**, select **"YCCC VR Lab Server"** from the dropdown
4. Browse and download educational VR applications
5. Install directly on your headset

### Default Configuration:
- The app now defaults to **YCCC VR Lab Server** for new installations
- Existing users can switch in Settings → Server type → "YCCC VR Lab Server"
- No address configuration needed - works automatically

## 🏫 YCCC VR Lab Integration

### Educational Focus
- Curated collection of educational VR applications
- Focus on nursing, healthcare, and professional training
- Safe, vetted content for academic environments

### Server Infrastructure
- **Primary Server**: `https://picozen-api.netlify.app`
- **Web Interface**: `https://ycccrlab.github.io/PicoZen-Web/`
- **Backup Systems**: Automatic fallback to local content

### API Endpoints
- `GET /api/apps` - List all available VR apps
- `GET /api/download/:id` - Download specific app APK
- `GET /api/categories` - Browse by category
- Full REST API for integration

## 🔧 Technical Details

### New Components Added:
- `YCCCServerProvider.java` - Handles connection to YCCC server
- Updated `SideloadAdapter.java` - Includes YCCC server option
- Enhanced UI strings for better user experience

### Version Information:
- **Version**: 0.7.5-yccc
- **Build**: Based on original PicoZen v0.7.4
- **Compatibility**: Full backward compatibility maintained

### Network Requirements:
- Internet connection required for app downloads
- Automatic HTTPS connection to YCCC servers
- Fallback handling for offline scenarios

## 🌐 Related Projects

- **[PicoZen-Server](https://github.com/YCCCVRLab/PicoZen-Server)** - Backend API server
- **[PicoZen-Web](https://github.com/YCCCVRLab/PicoZen-Web)** - Web interface
- **[Original PicoZen](https://github.com/barnabwhy/PicoZen)** - Original project by barnabwhy

## 📞 Support

### YCCC VR Lab
- **Location**: Room 112, Wells Campus
- **Institution**: York County Community College
- **GitHub**: [YCCCVRLab](https://github.com/YCCCVRLab)

### Getting Help
- Report issues via GitHub Issues
- Contact VR Lab for educational support
- Community support via Discord

## 📄 License

This enhanced version maintains the GPL-3.0 license of the original PicoZen project.

## 🙏 Acknowledgments

- **barnabwhy** - Original PicoZen creator and maintainer
- **UbiSim** - Educational VR simulation platform
- **YCCC VR Lab** - Educational enhancements and server infrastructure
- **VR Education Community** - Ongoing feedback and support

---

**Building the Future of VR Education** 🥽  
*YCCC VR Lab - York County Community College*