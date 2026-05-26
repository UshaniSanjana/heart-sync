import torch
import torch.nn as nn
import torch.nn.functional as F
from torchvision.models import mobilenet_v3_large

class ChannelAttention(nn.Module):
    def __init__(self, in_planes, ratio=16):
        super(ChannelAttention, self).__init__()
        self.fc1 = nn.Conv2d(in_planes, in_planes // ratio, 1, bias=False)
        self.relu1 = nn.ReLU()
        self.fc2 = nn.Conv2d(in_planes // ratio, in_planes, 1, bias=False)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        avg_pool = torch.mean(x, dim=(2, 3), keepdim=True)
        max_pool = torch.amax(x, dim=(2, 3), keepdim=True)
        avg_out = self.fc2(self.relu1(self.fc1(avg_pool)))
        max_out = self.fc2(self.relu1(self.fc1(max_pool)))
        out = avg_out + max_out
        return self.sigmoid(out)

class SpatialAttention(nn.Module):
    def __init__(self, kernel_size=7):
        super(SpatialAttention, self).__init__()
        padding = 3 if kernel_size == 7 else 1
        self.conv1 = nn.Conv2d(2, 1, kernel_size, padding=padding, bias=False)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        avg_out = torch.mean(x, dim=1, keepdim=True)
        max_out, _ = torch.max(x, dim=1, keepdim=True)
        x = torch.cat([avg_out, max_out], dim=1)
        x = self.conv1(x)
        return self.sigmoid(x)

class CBAM(nn.Module):
    def __init__(self, planes, ratio=16, kernel_size=7):
        super(CBAM, self).__init__()
        self.ca = ChannelAttention(planes, ratio)
        self.sa = SpatialAttention(kernel_size)

    def forward(self, x):
        out = x * self.ca(x)
        result = out * self.sa(out)
        return result

class DSConv(nn.Module):
    def __init__(self, in_channels, out_channels):
        super().__init__()
        self.depthwise = nn.Conv2d(in_channels, in_channels, kernel_size=3, padding=1, groups=in_channels, bias=False)
        self.pointwise = nn.Conv2d(in_channels, out_channels, kernel_size=1, bias=False)
        self.bn = nn.BatchNorm2d(out_channels)
        self.relu = nn.ReLU(inplace=True)

    def forward(self, x):
        x = self.depthwise(x)
        x = self.pointwise(x)
        x = self.bn(x)
        x = self.relu(x)
        return x

class DoubleDSConv(nn.Module):
    def __init__(self, in_channels, out_channels):
        super().__init__()
        self.double_conv = nn.Sequential(
            DSConv(in_channels, out_channels),
            DSConv(out_channels, out_channels)
        )

    def forward(self, x):
        return self.double_conv(x)

class MobileUNetv3(nn.Module):
    def __init__(self, n_classes, pretrained=True):
        super(MobileUNetv3, self).__init__()
        self.n_classes = n_classes
        self.encoder = mobilenet_v3_large(pretrained=pretrained).features
        
        self.up1 = nn.Upsample(scale_factor=2, mode='bilinear', align_corners=True)
        self.conv1 = DoubleDSConv(960 + 112, 512)
        self.att1 = CBAM(512)
        
        self.up2 = nn.Upsample(scale_factor=2, mode='bilinear', align_corners=True)
        self.conv2 = DoubleDSConv(512 + 40, 256)
        self.att2 = CBAM(256)
        
        self.up3 = nn.Upsample(scale_factor=2, mode='bilinear', align_corners=True)
        self.conv3 = DoubleDSConv(256 + 24, 128)
        self.att3 = CBAM(128)
        
        self.up4 = nn.Upsample(scale_factor=2, mode='bilinear', align_corners=True)
        self.conv4 = DoubleDSConv(128 + 16, 64)
        self.att4 = CBAM(64)
        
        self.up5 = nn.Upsample(scale_factor=2, mode='bilinear', align_corners=True)
        self.conv5 = DoubleDSConv(64 + 3, 32)
        self.att5 = CBAM(32)
        
        self.final_conv = nn.Conv2d(32, n_classes, kernel_size=1)

    def forward(self, x):
        x_0 = self.encoder[0:2](x)
        x_1 = self.encoder[2:4](x_0)
        x_2 = self.encoder[4:7](x_1)
        x_3 = self.encoder[7:13](x_2) 
        x_4 = self.encoder[13:](x_3)
        
        d1 = self.up1(x_4)
        diffY = x_3.size()[2] - d1.size()[2]
        diffX = x_3.size()[3] - d1.size()[3]
        d1 = F.pad(d1, [diffX // 2, diffX - diffX // 2, diffY // 2, diffY - diffY // 2])
        d1 = torch.cat([x_3, d1], dim=1)
        d1 = self.conv1(d1)
        d1 = self.att1(d1)
        
        d2 = self.up2(d1)
        diffY = x_2.size()[2] - d2.size()[2]
        diffX = x_2.size()[3] - d2.size()[3]
        d2 = F.pad(d2, [diffX // 2, diffX - diffX // 2, diffY // 2, diffY - diffY // 2])
        d2 = torch.cat([x_2, d2], dim=1)
        d2 = self.conv2(d2)
        d2 = self.att2(d2)
        
        d3 = self.up3(d2)
        diffY = x_1.size()[2] - d3.size()[2]
        diffX = x_1.size()[3] - d3.size()[3]
        d3 = F.pad(d3, [diffX // 2, diffX - diffX // 2, diffY // 2, diffY - diffY // 2])
        d3 = torch.cat([x_1, d3], dim=1)
        d3 = self.conv3(d3)
        d3 = self.att3(d3)
        
        d4 = self.up4(d3)
        diffY = x_0.size()[2] - d4.size()[2]
        diffX = x_0.size()[3] - d4.size()[3]
        d4 = F.pad(d4, [diffX // 2, diffX - diffX // 2, diffY // 2, diffY - diffY // 2])
        d4 = torch.cat([x_0, d4], dim=1)
        d4 = self.conv4(d4)
        d4 = self.att4(d4)
        
        d5 = self.up5(d4)
        diffY = x.size()[2] - d5.size()[2]
        diffX = x.size()[3] - d5.size()[3]
        d5 = F.pad(d5, [diffX // 2, diffX - diffX // 2, diffY // 2, diffY - diffY // 2])
        d5 = torch.cat([x, d5], dim=1)
        d5 = self.conv5(d5)
        d5 = self.att5(d5)
        
        out = self.final_conv(d5)
        return {'out': out, 'features': x_4}
