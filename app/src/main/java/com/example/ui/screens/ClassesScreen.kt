package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AriseClass
import com.example.data.ClassArchetype
import com.example.data.ClassCatalog
import com.example.ui.components.NeonCard
import com.example.ui.theme.*
import com.example.viewmodel.AriseViewModel

@Composable
fun ClassesScreen(
    viewModel: AriseViewModel
) {
    val profile by viewModel.playerProfile.collectAsState()
    var selectedArchetype by remember { mutableStateOf<ClassArchetype?>(null) }
    var inspectingClass by remember { mutableStateOf<AriseClass?>(null) }

    val filteredClasses = remember(selectedArchetype) {
        if (selectedArchetype == null) ClassCatalog.allClasses else ClassCatalog.allClasses.filter { it.archetype == selectedArchetype }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AriseVoidBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CLASS CODEX (49 ARCHETYPES)",
            color = AriseShadowViolet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Path of Ascension",
            color = AriseTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Archetype Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedArchetype == null,
                    onClick = { selectedArchetype = null },
                    label = { Text("All Classes (49)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AriseShadowViolet,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(ClassArchetype.values()) { arch ->
                FilterChip(
                    selected = selectedArchetype == arch,
                    onClick = { selectedArchetype = arch },
                    label = { Text(arch.title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AriseShadowViolet,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Classes List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filteredClasses) { cls ->
                val isCurrent = profile?.selectedClass.equals(cls.name, ignoreCase = true)

                NeonCard(
                    borderColor = if (isCurrent) AriseCyanNeon else AriseBorderGlow,
                    backgroundColor = if (isCurrent) Color(0xFF131D31) else AriseSurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { inspectingClass = cls }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cls.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cls.name,
                                    color = if (isCurrent) AriseCyanNeon else AriseTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "[CURRENT CLASS]",
                                        color = AriseCyanNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "${cls.archetype.title} • Weapon: ${cls.primaryWeapon}",
                                color = AriseTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ultimate: ${cls.ultimateAbility}",
                                color = AriseGoldRank,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    inspectingClass?.let { cls ->
        ClassDetailDialog(
            ariseClass = cls,
            isCurrent = profile?.selectedClass.equals(cls.name, ignoreCase = true),
            onDismiss = { inspectingClass = null },
            onSelectClass = {
                viewModel.setPlayerClass(cls.name)
                inspectingClass = null
            }
        )
    }
}

@Composable
fun ClassDetailDialog(
    ariseClass: AriseClass,
    isCurrent: Boolean,
    onDismiss: () -> Unit,
    onSelectClass: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            borderColor = AriseCyanNeon,
            backgroundColor = AriseVoidBlack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = ariseClass.iconEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = ariseClass.name, color = AriseCyanNeon, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(text = ariseClass.archetype.title, color = AriseTextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = ariseClass.lore, color = AriseTextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("• Core Attribute: ${ariseClass.coreAttribute}", color = AriseGoldRank, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("• Preferred Weapon: ${ariseClass.primaryWeapon}", color = AriseTextSecondary, fontSize = 12.sp)
                Text("• Armor: ${ariseClass.armorType}", color = AriseTextSecondary, fontSize = 12.sp)
                Text("• Passive: ${ariseClass.passiveSkill}", color = AriseEmeraldHeal, fontSize = 12.sp)
                Text("• Active: ${ariseClass.activeSkill}", color = AriseBlueMana, fontSize = 12.sp)
                Text("• Ultimate: ${ariseClass.ultimateAbility}", color = AriseShadowViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = AriseTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!isCurrent) {
                    Button(
                        onClick = onSelectClass,
                        colors = ButtonDefaults.buttonColors(containerColor = AriseCyanNeon)
                    ) {
                        Text("AWAKEN CLASS", color = AriseVoidBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
